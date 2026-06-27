"""In-memory record of processed notifications.

The worker owns no appointment data — only this log of what it processed, which
the /notifications endpoint exposes. Thread-safe: the consumer writes from a
background thread while the API reads from the request thread. Bounded so it
cannot grow without limit; newest first.
"""
import threading
from collections import deque


class NotificationStore:
    def __init__(self, max_size: int = 200):
        self._items: deque = deque(maxlen=max_size)
        self._lock = threading.Lock()

    def add(self, record: dict) -> None:
        with self._lock:
            self._items.appendleft(record)

    def list(self) -> list:
        with self._lock:
            return list(self._items)


store = NotificationStore()
