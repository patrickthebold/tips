import requests
def comment(comment):
    return {'comment': comment}
def tip(message):
    return {'message': message}

class Tips:
    
    def __init__(self, username, password, host = 'http://localhost:9000'):
        self.host = host
        self.session = requests.session()
        self.session.headers.update({'Csrf-Token': 'nocheck'})
        self.username = username
        self.password = password

    def cred(self):
        return {'username': self.username, 'password': self.password}
    def login(self):
        return self.session.post(f"{self.host}/login", json=self.cred())
    def logout(self):
        return self.session.post(f"{self.host}/logout")
    def create_user(self):
        return self.session.post(f"{self.host}/newUser", json=self.cred())
    def new_tip(self, message):
        return self.session.post(f"{self.host}/tip", json=tip(message))
    def update_tip(self, tip_id, message):
        return self.session.patch(f"{self.host}/tip/{tip_id}", json=tip(message))
    def new_comment(self, tip_id, comment_str):
        return self.session.post(f"{self.host}/tip/{tip_id}/comment", json=comment(comment_str))
    def update_comment(self, comment_id, comment_str):
        return self.session.patch(f"{self.host}/comment/{comment_id}", json=comment(comment_str))

    def tips(self):
        return self.session.get(f"{self.host}/tips")
    def comments(self, tip_id):
        return self.session.get(f"{self.host}/tip/{tip_id}/comments")
    def tip_history(self, tip_id):
        return self.session.get(f"{self.host}/tip/{tip_id}/history")
    def comment_history(self, comment_id):
        return self.session.get(f"{self.host}/comment/{comment_id}/history")
    def get_tip(self, tip_id, include_comments = True):
        return self.session.get(f"{self.host}/tip/{tip_id}", params={'includeComments': 'true' if include_comments else 'false'})
    def get_comment(self, comment_id):
        return self.session.get(f"{self.host}/comment/{comment_id}")
