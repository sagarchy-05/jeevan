"""Worker HTTP surface: health + the in-memory processed-notifications log."""
from fastapi import APIRouter

from app.config import settings
from app.store.notification_store import store

router = APIRouter()


@router.get("/health")
def health() -> dict:
    return {"status": "ok", "service": settings.app_name}


@router.get("/notifications")
def notifications() -> list:
    """Processed notification events, newest first — proof the worker is doing real work."""
    return store.list()
