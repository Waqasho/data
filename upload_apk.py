import requests

headers = {
    'Accept': '*/*',
    'Accept-Language': 'en-GB',
    'Connection': 'keep-alive',
    'Origin': 'https://gofile.io',
    'Referer': 'https://gofile.io/',
    'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36',
    'sec-ch-ua': '"Chromium";v="127", "Not)A;Brand";v="99", "Microsoft Edge Simulate";v="127", "Lemur";v="127"',
    'sec-ch-ua-mobile': '?1',
    'sec-ch-ua-platform': '"Android"',
}

# Actual file path
file_path = "/workspace/naxobrowser-unlimited-windows.apk"

# Read actual file
with open(file_path, 'rb') as f:
    files = {
        'token': (None, 'AKaeLgJWYitZIB7YEsymZvu4HFX8rXzm'),  # Your Gofile API token
        'folderId': (None, '847f384f-1184-4f1b-818a-e66c3b3be2aa'),  # Optional: Target folder
        'file': ('naxobrowser-unlimited-windows.apk', f, 'application/vnd.android.package-archive'),
    }

    response = requests.post('https://upload.gofile.io/uploadfile', headers=headers, files=files)

# Print response
try:
    print("Status Code:", response.status_code)
    print("JSON Response:\n", response.json())
except Exception as e:
    print("❌ Error decoding response:", e)
    print("Raw response:\n", response.text)