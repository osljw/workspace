from pynput import mouse, keyboard
import time
from queue import Queue
import logging
from recoder import MouseEvent, KeyBoardEvent
        
class Actions:
    def __init__(self):
        pass
    def load_data(self, filename):
        pass


class MouseExecuter:
    def __init__(self):
        self.mouse_controller = mouse.Controller()

    def execute(self, event):
        if event.action == "move":
            logging.debug("move: {}, {}".format(event.x, event.y))
            self.mouse_controller.position = (event.x, event.y)
        elif event.action == "scroll":
            self.mouse_controller.scroll(event.x, event.y)
        elif event.action == "click":
            if event.pressed == True:
                self.mouse_controller.press(event.button)
            elif event.pressed == False:
                self.mouse_controller.press(event.button)
            #self.mouse_controller.click(mouse.Button.left, 2)

class KeyBoardExecuter:
    def __init__(self):
        self.mouse_controller = mouse.Controller()

    def execute(self, event):
        pass
        

class Executer:
    def __init__(self, event_list = None):
        self.event_list = event_list
        self.mouse_executer = MouseExecuter()
        self.keyboard_executer = KeyBoardExecuter()

    def execute(self, event):
        if isinstance(event, MouseEvent):
            self.mouse_executer.execute(event)
        elif isinstance(event, KeyBoardEvent):
            self.keyboard_executer.execute(event)

    def run(self):
        for event in self.event_list:
            time.sleep(0.1)
            self.execute(event)

    def parse_event(self, line):
        line = line.strip().split(',')
        data = dict(x.split('=') for x in line)
        if data['action'] in ["move", "click", "scroll"]:
            action = data.get("action")
            x = int(data.get("x"))
            y = int(data.get("y"))
            button = data.get("button", None)
            pressed = data.get("pressed", None)
            time = data.get("time", None)
            return MouseEvent(action, x, y, button, pressed, time)
        elif data["action"] in ["press", "release"]:
            action = data.get("action")
            key = data.get("key", None)
            return KeyBoardEvent(action, key)

    def load_data(self, filename):
        self.event_list = []
        with open(filename, 'r') as fd:
            for line in fd:
                event = self.parse_event(line)
                self.event_list.append(event)
    

if __name__ == "__main__":
    # q = Queue()
    # q.put(MouseEvent("move", 0,0))
    # q.put(MouseEvent("move", 10,10))
    # q.put(MouseEvent("move", 50,50))
    # q.put(MouseEvent("move", 100,100))
    # q.put(MouseEvent("move", 200,200))
    #executer = Executer(list(q.queue))

    executer = Executer()
    executer.load_data("recoder_info.txt")
    executer.run()
