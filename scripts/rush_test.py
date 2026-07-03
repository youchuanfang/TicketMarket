import json
import subprocess
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:8080"
STOCK_KEY = "ticket:batch:3001:level:2001:stock"
STOCK = 5
USERS = 20


def request(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            return payload["data"]
    except urllib.error.HTTPError as exc:
        payload = json.loads(exc.read().decode("utf-8"))
        return {"status": "HTTP_ERROR", "message": payload.get("message")}


def login(username, password):
    return request("POST", "/api/auth/login", {"username": username, "password": password})["token"]


def ensure_user(index):
    username = f"stress{index:02d}"
    password = "user123"
    try:
        request("POST", "/api/auth/register", {"username": username, "password": password, "nickname": f"压测用户{index:02d}"})
    except Exception:
        pass
    token = login(username, password)
    request("POST", "/api/user/real-name", {"realName": f"压测用户{index:02d}", "idCard": f"33010219990101{index:04d}"}, token=token)
    viewers = request("GET", "/api/user/viewers", token=token)
    if not viewers:
        viewer = request("POST", "/api/user/viewers", {"name": f"压测用户{index:02d}", "idCard": f"33010219990101{index:04d}", "phone": f"1390000{index:04d}"}, token=token)
    else:
        viewer = viewers[0]
    return token, viewer["id"]


def submit(index):
    token, viewer_id = ensure_user(index)
    return request("POST", "/api/rush/submit", {
        "sessionId": 1001,
        "batchId": 3001,
        "ticketLevelId": 2001,
        "quantity": 1,
        "viewerIds": [viewer_id]
    }, token=token)


def redis(*args):
    return subprocess.check_output(["docker", "exec", "redis-ticket", "redis-cli", *args], text=True).strip()


def main():
    admin = login("admin", "admin123")
    request("POST", "/api/admin/sale-batches/3001/init-redis-stock", token=admin)
    redis("set", STOCK_KEY, str(STOCK))
    started = time.time()
    results = []
    with ThreadPoolExecutor(max_workers=USERS) as pool:
        futures = [pool.submit(submit, i) for i in range(1, USERS + 1)]
        for future in as_completed(futures):
            results.append(future.result())
    success = sum(1 for item in results if item.get("status") == "SUCCESS")
    failed = USERS - success
    remaining = int(redis("get", STOCK_KEY) or "0")
    print(json.dumps({
        "users": USERS,
        "initialStock": STOCK,
        "success": success,
        "failed": failed,
        "remainingStock": remaining,
        "durationSeconds": round(time.time() - started, 2),
        "passed": success <= STOCK and remaining >= 0
    }, ensure_ascii=False, indent=2))
    if success > STOCK or remaining < 0:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
