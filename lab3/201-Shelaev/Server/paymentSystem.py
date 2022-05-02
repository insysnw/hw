
import service

from flask_httpauth import HTTPBasicAuth 
from flask import Flask, request, jsonify, abort, make_response, url_for

app = Flask(__name__)

auth = HTTPBasicAuth()

wsgi_app = app.wsgi_app

SERVER_HOST = '127.0.0.1'
SERVER_PORT = 8080

@app.errorhandler(400)
def bad_request(error):
    return make_response(jsonify({'ERROR': 'Invilid request'}), 400)

@app.errorhandler(401)
def unauthorized():
    return make_response(jsonify({'ERROR': 'Unauthorized access'}), 401)

@app.errorhandler(403)
def forbidden():
    return make_response(jsonify({'ERROR': 'The method is prohibited, there is not enough data'}), 403)

@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'ERROR': 'The requested data was not found'}), 404)

@auth.verify_password
def verify_password(login, password):
    return service.verify_password(login, password)

@app.route('/login', methods = ['POST'])
def login():
    req = request.json
    if not (req and 'login' in req and 'password' in req):
        abort(403)
    if service.verify_password(req['login'], req['password']) is None:
        abort(404)
    return jsonify({'login': req['login']}), 200

@app.route('/logout', methods = ['POST'])
@auth.login_required
def logout():
    req = request.json
    if not (req and 'login' in req):
        abort(403)
    return jsonify({'logout': req['login']}), 200

@app.route('/create/client/', methods = ['POST'])
def create_client():
    req = request.json
    if not (req and 'login' in req and 'password' in req):
        abort(403)
    wallet = service.create_client(req['login'], req['password'])
    if wallet is None:
        abort(400)
    return jsonify({'wallet': wallet}), 201

@app.route('/create/wallet/', methods = ['POST'])
@auth.login_required
def create_wallet():
    req = request.json
    if not (req and 'login' in req):
        abort(403)
    wallet = service.create_wallet(req['login'])
    if wallet is None:
        abort(404)
    return jsonify({'wallet': wallet}), 201

@app.route('/create/payment/', methods = ['POST'])
@auth.login_required
def create_payment():
    req = request.json
    if not (req and 'login' in req and 'wallet' in req and 'sum' in req):
        abort(403)
    result = service.create_payment(req['login'], req['wallet'], req['sum'])
    if result is None:
        abort(404)
    elif result == -1:
        abort(400)
    return jsonify({'wallet': result[0], 'account': result[1], 'new_account': result[2]}), 201

@app.route('/create/payment/transfer', methods = ['POST'])
@auth.login_required
def do_transfer():
    req = request.json
    if not (req and 'from_wallet' in req and 'to_wallet' in req and 'sum' in req):
        abort(403)
    result = service.do_transfer(req['from_wallet'], req['to_wallet'], req['sum'])
    if result is None:
        abort(404)
    return jsonify({'wallet': result[0], 'account': result[1]}), 201

@app.route('/get/wallets/', methods = ['GET'])
@auth.login_required
def get_wallets():
    req = request.json
    if not (req and 'login' in req):
        abort(403)
    wallets = service.get_wallets(req['login'])
    if wallets is None:
        abort(404)
    return jsonify({'wallets': wallets})

@app.route('/get/wallet/<int:wallet>', methods = ['GET'])
@auth.login_required
def get_account(wallet):
    req = request.json
    if not (req and 'login' in req):
        abort(403)
    account = service.get_account(req['login'], wallet)
    if account is None:
        abort(404)
    return jsonify({'account': account})

if __name__ == '__main__':
    app.run(SERVER_HOST, SERVER_PORT)
