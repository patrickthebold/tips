from tips import Tips
import random, string

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

def test_index():
    t = random_tips()
    r = t.index()
    assert ok(r)
    r = r.json()
    assert "login_ref" in r
    assert "create_user_ref" in r
    assert "logout_ref" in r
    assert "get_tips_ref" in r
    assert "post_new_tip_ref" in r

def test_create_user():
   t = random_tips()
   assert ok(t.create_user())
   assert bad_request(t.create_user())

def test_login_logout():
    t = random_tips()
    assert forbidden(t.tips())
    assert bad_request(t.login()) # User Not created
    assert ok(t.create_user())
    assert ok(t.tips())
    assert ok(t.logout())
    assert forbidden(t.tips()) # logged out
    assert ok(t.login())
    assert ok(t.tips())
    assert ok(t.logout())
    good_password = t.password
    t.password = "bad"
    assert bad_request(t.login()) # Bad password
    assert forbidden(t.tips())
    t.password = good_password 
    assert ok(t.login())
    t.password = "bad"
    assert bad_request(t.login())
    assert ok(t.tips()) # Still logged into old session. (should session expire?)

def test_create_tips_and_comments():
    message = random_string()
    api = get_api()
    r = api.new_tip(message)
    assert ok(r)
    assert 'tip_ref' in r.json()
    assert 'tip_comments_ref' in r.json()
    assert 'post_new_comment_ref' in r.json()
    assert 'tip_history_ref' in r.json()
    tip_id = r.json()['tipId']

    # get tip
    r = api.get_tip(tip_id)
    assert ok(r)
    tip = r.json()
    assert tip['created'] == tip['modified']
    assert tip['username'] == api.username
    assert tip['comments'] == []
    assert tip['message'] == message

    # next tip does not exist
    next_tip_id = tip_id + 1
    r = api.get_tip(next_tip_id)
    assert not_found(api.get_tip(next_tip_id))
    assert not_found(api.update_tip(next_tip_id, random_string()))
    assert not_found(api.comments(next_tip_id))
    assert not_found(api.tip_history(next_tip_id))
    assert not_found(api.new_comment(next_tip_id, random_string()))

    # Assert comments
    r = api.comments(tip_id)
    assert ok(r)
    assert r.json() == []

    # assert history
    r = api.tip_history(tip_id)
    assert ok(r)
    history = r.json()
    assert history['tipId'] == tip_id
    assert len(history['versions']) == 1
    hc = history['versions'][0]
    assert tip['modified'] == hc['modified']
    assert tip['username'] == hc['username']
    assert tip['message'] == hc['message']
    
    # update tip
    next_message = random_string()
    r = api.update_tip(tip_id, next_message)
    assert ok(r)
    # assert updated tip (check modfied time)
    r = api.get_tip(tip_id)
    assert ok(r)
    next_tip = r.json()
    assert next_tip['created'] != next_tip['modified']
    assert next_tip['username'] == api.username
    assert next_tip['comments'] == []
    assert next_tip['message'] == next_message
    # assert history
    r = api.tip_history(tip_id)
    assert ok(r)
    history = r.json()
    assert history['tipId'] == tip_id
    assert len(history['versions']) == 2
    hc = history['versions'][1]
    assert tip['modified'] == hc['modified']
    assert tip['username'] == hc['username']
    assert tip['message'] == hc['message']
    hc = history['versions'][0]
    assert next_tip['modified'] == hc['modified']
    assert next_tip['username'] == hc['username']
    assert next_tip['message'] == hc['message']
    
    # add comment
    comment = random_string()
    r = api.new_comment(tip_id, comment)
    assert ok(r)
    comment_id = r.json()["commentId"]
    assert 'comment_ref' in r.json()
    assert 'comment_history_ref' in r.json()

    # query without comments (check modified time)
    r = api.get_tip(tip_id, False)
    assert ok(r)
    assert "comments" not in r.json()
    modified_after_comments = r.json()["modified"]

    # query with comments
    r = api.get_tip(tip_id, True)
    assert ok(r)
    assert modified_after_comments == r.json()["comments"][0]["created"]
    assert comment_id == r.json()["comments"][0]["commentId"]
    assert comment == r.json()["comments"][0]["comment"]
    assert len(r.json()["comments"]) == 1

    # assert comment
    r = api.get_comment(comment_id)
    assert ok(r)
    commentObj = r.json()
    assert commentObj["commentId"] == comment_id
    assert commentObj["comment"] == comment
    assert commentObj["username"] == api.username
    assert commentObj["created"] == commentObj["modified"]

    # assert no next comment
    next_comment_id = comment_id + 1
    assert not_found(api.get_comment(next_comment_id))
    assert not_found(api.update_comment(next_comment_id, random_string()))
    assert not_found(api.comment_history(next_comment_id))
    
    # update comment
    next_comment = random_string()
    r = api.update_comment(comment_id, next_comment)
    assert ok(r)

    # assert updated comment (check modfied time)
    r = api.get_comment(comment_id)
    assert ok(r)
    next_commentObj = r.json()
    assert next_commentObj['created'] != next_commentObj['modified']
    assert next_commentObj['username'] == api.username
    assert next_commentObj['comment'] == next_comment
    
    # assert comment history
    r = api.comment_history(comment_id)
    assert ok(r)
    history = r.json()
    assert history['commentId'] == comment_id
    assert len(history['versions']) == 2
    hc = history['versions'][1]
    assert commentObj['modified'] == hc['modified']
    assert commentObj['username'] == hc['username']
    assert commentObj['comment'] == hc['comment']
    hc = history['versions'][0]
    assert next_commentObj['modified'] == hc['modified']
    assert next_commentObj['username'] == hc['username']
    assert next_commentObj['comment'] == hc['comment']

    # Different user modification
    other_user = get_api()
    print(api.username)
    print(other_user.username)
    assert forbidden(other_user.update_tip(tip_id, random_string()))
    assert not_found(other_user.update_tip(next_tip_id, random_string()))
    assert forbidden(other_user.update_comment(comment_id, random_string()))
    assert not_found(other_user.update_comment(next_comment_id, random_string()))

    # get all tips across users.
    other_user.login()
    other_user_message = random_string()
    other_user.new_tip(other_user_message)
    r = other_user.tips()
    assert ok(r)
    all_tips = r.json()
    assert all_tips[0]['username'] == other_user.username
    assert all_tips[0]['message'] == other_user_message
    assert all_tips[1]['username'] == api.username
    assert all_tips[1]['message'] == next_message

