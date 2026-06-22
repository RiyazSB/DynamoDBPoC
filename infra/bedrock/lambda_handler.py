import json
import os
from urllib.parse import urlencode
import urllib3

http = urllib3.PoolManager()


def _extract_json_body(request_body):
    """Normalize Bedrock requestBody into API payload dict.

    Supports multiple shapes observed in action-group invocations:
    - Direct dict body: {"name": "x", "description": "y"}
    - content/properties list
    - content/application-json/body as JSON string
    """
    if not request_body:
        return None

    payload = None

    if isinstance(request_body, dict) and "content" not in request_body:
        payload = dict(request_body)
    else:
        content = request_body.get("content", {}) if isinstance(request_body, dict) else {}
        app_json = content.get("application/json", {})

        # Variant 1: properties list
        props = app_json.get("properties", [])
        if isinstance(props, list) and props:
            out = {}
            for p in props:
                name = p.get("name")
                value = p.get("value")
                if name is not None:
                    out[name] = value
            payload = out if out else None

        # Variant 2: body field
        if payload is None and isinstance(app_json, dict) and "body" in app_json:
            body = app_json.get("body")
            if isinstance(body, str) and body.strip():
                try:
                    parsed = json.loads(body)
                    if isinstance(parsed, dict):
                        payload = parsed
                except json.JSONDecodeError:
                    payload = None
            elif isinstance(body, dict):
                payload = body

    if not isinstance(payload, dict):
        return None

    # Backward compatibility: some prompts/tools still use `data` instead of `name`.
    if (not payload.get("name")) and payload.get("data"):
        payload["name"] = payload["data"]

    return payload


def _substitute_path_params(api_path, parameters):
    if not parameters or not api_path:
        return api_path, parameters or []
    remaining = []
    for p in parameters:
        name = p.get("name", "")
        value = p.get("value", "")
        placeholder = "{" + name + "}"
        if placeholder in api_path:
            api_path = api_path.replace(placeholder, str(value))
        else:
            remaining.append(p)
    return api_path, remaining


def _extract_query_params(parameters):
    if not parameters:
        return {}
    result = {}
    for p in parameters:
        name = p.get("name")
        value = p.get("value")
        if name and value is not None:
            result[name] = value
    return result


def lambda_handler(event, context):
    original_api_path = event.get("apiPath", "")
    original_http_method = event.get("httpMethod", "GET")
    action_group = event.get("actionGroup", "")

    try:
        api_endpoint = os.environ["API_ENDPOINT"].rstrip("/")
        http_method = (original_http_method or "GET").upper()
        parameters = event.get("parameters") or []

        resolved_path, remaining_params = _substitute_path_params(original_api_path, parameters)

        query_params = _extract_query_params(remaining_params)
        body_obj = _extract_json_body(event.get("requestBody"))

        url = api_endpoint + resolved_path
        if query_params:
            url = url + "?" + urlencode(query_params)

        headers = {"Content-Type": "application/json"}
        print("Calling: " + http_method + " " + url)

        if http_method in ("POST", "PUT", "PATCH"):
            body_bytes = json.dumps(body_obj or {}).encode("utf-8")
            resp = http.request(http_method, url, body=body_bytes, headers=headers)
        else:
            resp = http.request(http_method, url, headers=headers)

        response_body = resp.data.decode("utf-8") if resp.data else "{}"
        print("Response: " + str(resp.status) + " - " + response_body)

        return {
            "messageVersion": "1.0",
            "response": {
                "actionGroup": action_group,
                "apiPath": original_api_path,
                "httpMethod": http_method,
                "httpStatusCode": int(resp.status),
                "responseBody": {
                    "application/json": {
                        "body": response_body
                    }
                }
            }
        }

    except Exception as e:
        import traceback
        traceback.print_exc()
        return {
            "messageVersion": "1.0",
            "response": {
                "actionGroup": action_group,
                "apiPath": original_api_path,
                "httpMethod": original_http_method,
                "httpStatusCode": 500,
                "responseBody": {
                    "application/json": {
                        "body": json.dumps({"error": str(e)})
                    }
                }
            }
        }
