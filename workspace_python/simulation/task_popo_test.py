#-*- coding:utf-8 -*-

import os
import time
import pyautogui
import pyperclip


work_dir=os.path.dirname(os.path.abspath(__file__))
print("work_dir:", work_dir)
data_file = os.path.join(work_dir, "data.txt")
 
def zhifou_send(data):
    #data = "【#" + data + "#】"
    pyautogui.hotkey("ctrl", "alt", "s")
    pyautogui.hotkey("ctrl", "a")
    pyautogui.press(["backspace"]*10)
    pyautogui.typewrite("zhifou")
    pyautogui.press("enter")
    time.sleep(1)
    pyautogui.press("enter")
    pyautogui.press("enter")
    time.sleep(1)
    pyperclip.copy(data)
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


def main():
    all_data = []
    count = 0
    while True:
        data = read_data(data_file)
        all_data = update_data(all_data, data)
        zhifou_send(all_data[count])
        print("send {}th, {}".format(count, all_data[count]))
        count += 1
        if(count >= len(all_data)): count=0
        time.sleep(3)
        #time.sleep(24*60*60)


if __name__ == '__main__':
    main()
##    print("start")
##    data = read_data(data_file)
##    print(data)
