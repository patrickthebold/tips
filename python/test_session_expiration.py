import os, time
from utilities import *

def test_session_expires():
    t = get_api()
    assert ok(t.tips())
    session_timeout = int(os.environ['TIPS_SESSION_TIMEOUT'])/1000
    time.sleep(session_timeout - 100)
    assert ok(t.tips())
    time.sleep(session_timeout)
    assert forbidden(t.tips())

