import random, string
from tips import Tips

def random_string():
    return ''.join(random.choice(string.printable) for i in range(64))

def random_tips():
    return Tips(random_string(), random_string())

def get_api():
    t = random_tips()
    t.create_user()
    return t

def ok(resp):
    return resp.status_code == 200
def bad_request(resp):
    return resp.status_code == 400
def forbidden(resp):
    return resp.status_code == 403
def not_found(resp):
    return resp.status_code == 404

