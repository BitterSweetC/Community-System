import os
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import time
import hashlib
import re
import sys

# Configuration
START_URLS = [
    "https://tw.shiep.edu.cn/",  # 团委网站（社团信息主要来源）
    "https://www.shiep.edu.cn/"  # 学校主页
]
ALLOWED_DOMAINS = ["shiep.edu.cn"]
# 社团相关关键词
KEYWORDS = [
    "社团", "协会", "学生会", "团委", "招新", "活动", "Club", "Association", 
    "社团联合会", "社团管理", "社长", "文体", "社会实践", "志愿者", "义工",
    "第二课堂", "素质拓展"
]

# 获取当前脚本所在目录
AGENT_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(AGENT_DIR, "data")
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
MAX_PAGES = 100  # 限制爬取页面数量，测试通过后可调大

# Create data directory
if not os.path.exists(DATA_DIR):
    os.makedirs(DATA_DIR)
    print(f"Created data directory: {DATA_DIR}")

visited_urls = set()
urls_to_visit = list(START_URLS)

def is_valid_url(url):
    try:
        parsed = urlparse(url)
        # Must be http or https
        if parsed.scheme not in ['http', 'https']:
            return False
        # Must have a network location
        if not parsed.netloc:
            return False
        # Check if domain ends with any of the allowed domains
        return any(parsed.netloc.endswith(domain) for domain in ALLOWED_DOMAINS)
    except:
        return False

def is_relevant(title, content):
    """Check if the page content is relevant to clubs/associations."""
    combined_text = (title + " " + content).lower()
    for keyword in KEYWORDS:
        if keyword.lower() in combined_text:
            return True
    return False

def clean_filename(url):
    # Use MD5 hash of URL for filename to avoid special characters and length issues
    return hashlib.md5(url.encode('utf-8')).hexdigest() + ".md"

def save_page(url, title, content):
    filename = clean_filename(url)
    filepath = os.path.join(DATA_DIR, filename)
    
    try:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"# {title}\n\n")
            f.write(f"**Source URL**: {url}\n")
            f.write(f"**Date**: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            f.write("---\n\n")
            f.write(content)
        print(f"Saved: {filename} ({title})")
    except Exception as e:
        print(f"Error saving {url}: {e}")

def get_text_content(soup):
    # Remove script, style, nav, footer, header elements
    for element in soup(["script", "style", "nav", "footer", "header", "noscript"]):
        element.extract()
    
    # Get text
    text = soup.get_text()
    
    # Break into lines and remove leading/trailing space on each
    lines = (line.strip() for line in text.splitlines())
    # Break multi-headlines into a line each
    chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
    # Drop blank lines
    text = '\n\n'.join(chunk for chunk in chunks if chunk)
    return text

def crawl():
    headers = {'User-Agent': USER_AGENT}
    count = 0
    
    while urls_to_visit and count < MAX_PAGES:
        current_url = urls_to_visit.pop(0)
        
        if current_url in visited_urls:
            continue
            
        print(f"[{count+1}/{MAX_PAGES}] Crawling: {current_url}")
        
        try:
            response = requests.get(current_url, headers=headers, timeout=10)
            
            if response.status_code != 200:
                print(f"Failed: {response.status_code}")
                visited_urls.add(current_url)
                continue
                
            # Content Type check
            content_type = response.headers.get('Content-Type', '').lower()
            if 'text/html' not in content_type:
                print(f"Skipping non-HTML content: {content_type}")
                visited_urls.add(current_url)
                continue

            response.encoding = response.apparent_encoding
            soup = BeautifulSoup(response.text, 'html.parser')
            
            title = soup.title.string.strip() if soup.title else "No Title"
            content = get_text_content(soup)
            
            # Only save if substantial content is found (>100 chars) and relevant
            if len(content) > 100:
                if is_relevant(title, content):
                    save_page(current_url, title, content)
                    count += 1
                else:
                    print(f"Skipping: Not relevant to clubs/associations ({title})")
            else:
                print("Skipping: Content too short")
            
            # Extract links
            for link in soup.find_all('a', href=True):
                href = link['href']
                full_url = urljoin(current_url, href)
                full_url = full_url.split('#')[0].rstrip('/') # Normalize
                
                if is_valid_url(full_url) and full_url not in visited_urls and full_url not in urls_to_visit:
                    urls_to_visit.append(full_url)
            
            visited_urls.add(current_url)
            time.sleep(1) # Politeness delay
            
        except Exception as e:
            print(f"Error: {e}")
            visited_urls.add(current_url)

if __name__ == "__main__":
    print(f"Starting crawl for {START_URLS}")
    print(f"Data will be saved to {DATA_DIR}")
    crawl()
    print("Crawl finished.")
