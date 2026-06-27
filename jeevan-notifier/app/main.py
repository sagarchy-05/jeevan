"""Jeevan notifier — FastAPI app.

On startup it launches the blocking pika consumer on a daemon background thread
(simpler than async for this scope); on shutdown it asks the consumer to stop.
"""
import logging
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import router
from app.messaging.consumer import NotificationConsumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer = NotificationConsumer()
    thread = threading.Thread(target=consumer.run, name="notifier-consumer", daemon=True)
    thread.start()
    app.state.consumer = consumer
    try:
        yield
    finally:
        consumer.stop()


app = FastAPI(title="Jeevan Notifier", version="0.1.0", lifespan=lifespan)
app.include_router(router)
