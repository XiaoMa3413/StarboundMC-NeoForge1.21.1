"""Run an authoring script inside the locally running Blockbench renderer.

Start Blockbench with --remote-debugging-port=9228. Requires websocket-client.
The debugger is bound to localhost; no external service is involved.
"""
import argparse
from contextlib import closing
import json
from pathlib import Path
import urllib.request
import websocket


def evaluate(expression):
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    with opener.open('http://127.0.0.1:9228/json/list') as response:
        targets = json.load(response)
    target = next(t for t in targets if t['type'] == 'page' and 'index.html' in t['url'])
    with closing(websocket.create_connection(target['webSocketDebuggerUrl'], origin='http://localhost:9228', timeout=180)) as connection:
        connection.send(json.dumps({'id': 1, 'method': 'Runtime.evaluate', 'params': {
            'expression': expression, 'awaitPromise': True, 'returnByValue': True, 'userGesture': True}}))
        while True:
            message = json.loads(connection.recv())
            if message.get('id') != 1:
                continue
            if 'error' in message:
                raise RuntimeError(json.dumps(message['error']))
            result = message['result']
            if 'exceptionDetails' in result:
                raise RuntimeError(json.dumps(result['exceptionDetails'], ensure_ascii=True))
            return result.get('result', {}).get('value')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--file', type=Path)
    parser.add_argument('--eval')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    value = evaluate(args.file.read_text(encoding='utf-8') if args.file else args.eval)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({'output': str(args.output)}, ensure_ascii=True))
    else:
        print(json.dumps(value, ensure_ascii=True))
