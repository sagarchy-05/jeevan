"""Jeevan notifier — FastAPI app.

Step 1 scaffold: a health endpoint only. The RabbitMQ consumer (started on a
background thread via a startup hook), the notification service, and the
/notifications endpoint are added in later build steps.
"""
from fastapi import FastAPI

from app.config import settings

app = FastAPI(title="Jeevan Notifier", version="0.1.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": settings.app_name}
