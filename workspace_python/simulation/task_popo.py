#-*- coding:utf-8 -*-

import os
import sys
import time
import datetime
import pyautogui
import pyperclip
from io import BytesIO
import win32clipboard
import threading
import chardet

work_dir=os.path.dirname(os.path.abspath(__file__))
print("work_dir:", work_dir)
data_file = os.path.join(work_dir, "data.txt")


def get_from_clipboard():
    win32clipboard.OpenClipboard()
    copy_text = win32clipboard.GetClipboardData(win32clipboard.CF_TEXT)
    win32clipboard.CloseClipboard()
    return copy_text


def is_send_success(w):
    x, y = w.left + w.width//2, w.top + w.height//2
    pyautogui.click(x=x, y=y)
    pyautogui.hotkey("ctrl", "a")
    pyautogui.hotkey("ctrl", "c")
    
    data = get_from_clipboard()
    #encodeing = chardet.detect(data)
    #data = data[-100:]
    try:
        #data = data.decode('GB2312')
        data = data.decode('gbk')
    except UnicodeDecodeError:
        print("====UnicodeDecodeError===")
        return False
        
    content = data.strip().split('\n')
    print("reply_content:", content[-1])
    if "内容重复了" in content[-1]:
        return False
    elif '成功' in content[-1]:
        return True
    else:
        return False
    

def send_to_clipboard(img):
    output = BytesIO()
    img.convert("RGB").save(output, "BMP")
    data = output.getvalue()[14:]
    output.close()
    
    win32clipboard.OpenClipboard()
    win32clipboard.EmptyClipboard()
    win32clipboard.SetClipboardData(win32clipboard.CF_DIB, data)
    win32clipboard.CloseClipboard()

 
def zhifou_send(data):
    data = "【#" + data + "#】"
    pyautogui.hotkey("ctrl", "alt", "s")
    pyautogui.hotkey("ctrl", "a")
    pyautogui.press(["backspace"]*10)
    pyautogui.typewrite("zhifou")
    pyautogui.press("enter")
    #pyautogui.press("enter")
    time.sleep(2)
    
    # open dialog
    pyautogui.press("enter")
    time.sleep(2)
    
    # clean date content
    pyautogui.hotkey("ctrl", "a")
    pyautogui.press(["backspace"]*3)
    time.sleep(1)
    pyautogui.press("enter")
    
    # send content
    pyperclip.copy(data)
    pyautogui.hotkey('ctrl', 'v')
    time.sleep(1)
    print("send:", data)
    pyautogui.press("enter")

    # send date
    now_time = datetime.datetime.now()
    now_time = now_time.strftime("%Y-%m-%d %H:%M:%S")
    pyautogui.typewrite(now_time)

    # screen shot
    w = pyautogui.getFocusedWindow()
    # wait reply
    time.sleep(5)
    status = is_send_success(w)

    im = pyautogui.screenshot(region=(w.topleft[0], w.topleft[1], w.width, w.height))
    return status, im


def screenshot_send(data):
    pyautogui.hotkey("ctrl", "alt", "s")
    pyautogui.hotkey("ctrl", "a")
    pyautogui.press(["backspace"]*10)
    pyautogui.typewrite("meiri")
    #pyautogui.typewrite("zhifou")
    pyautogui.press("enter")

    time.sleep(1)
    pyautogui.press("enter")
    pyautogui.press("enter")
    time.sleep(1)
    send_to_clipboard(data)
    #pyperclip.copy(data)
    pyautogui.hotkey('ctrl', 'v')
    time.sleep(1)
    print("send")
    pyautogui.press("enter")


def read_data(data_file):
    data = []
    fd = open(data_file, encoding="utf-8")
    for line in fd:
        line = line.strip()
        if len(line) < 10: continue
        data.append(line.strip())
    fd.close()
    return data

def update_data(all_data, data):
    if len(all_data) < len(data):
        print("found new data: {}".format(len(data) - len(all_data)))
        all_data += data[len(all_data):]
    return all_data

def popo_task():
    all_data = []
    count = 5
    while True:
        data = read_data(data_file)
        all_data = update_data(all_data, data)
        is_success, im = zhifou_send(all_data[count])
        if not is_success:
            count += 1
            continue
        screenshot_send(im)
        print("send {}th/{}total, {}".format(count, len(all_data), all_data[count]))
        count += 1
        if(count >= len(all_data)):
            count=0
            sys.exit(1)
        #time.sleep(5)
        time.sleep(24*60*60)

def format_timedelta(td):
    minutes, seconds = divmod(td.seconds + td.days * 86400, 60)
    hours, minutes = divmod(minutes, 60)
    return '{:d}:{:02d}:{:02d}'.format(hours, minutes, seconds)

def start_task(task_func, start_time):
    # 获取现在时间
    now_time = datetime.datetime.now()
    start_time = datetime.datetime.strptime(start_time, "%Y-%m-%d %H:%M:%S")

    start_time = start_time - now_time
    if start_time.total_seconds() < 0:
        print("==== start_time set Error =======")
        #sys.exit(1)
    else:
        print("task will start after {}".format(format_timedelta(start_time)))

    timer = threading.Timer(start_time.total_seconds(), task_func)
    timer.start()
    timer.join()


def main():
    #start_time = "2019-05-28 10:40:00"
    start_time = "2019-07-03 09:00:00"
    start_task(popo_task, start_time)


if __name__ == '__main__':
    main()
