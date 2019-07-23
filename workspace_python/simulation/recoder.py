from pynput import mouse, keyboard
import time
from queue import Queue
import logging

stop_flag = False

class MouseEvent():
    def __init__(self, action, x, y, button=None, pressed=None, time=None):
        '''
        Args: 
            action: {"move", "click", "scroll"}
            x:

        '''
        self.action = action
        self.x = x
        self.y = y
        self.button = button
        self.pressed = pressed
        self.time = time

    def dump(self):
        return "action:{}, x={}, y={}\n".format(self.action, self.x, self.y)

class KeyBoardEvent():
    def __init__(self, action, key, time=None):
        self.action = action
        self.key = key
    def dump(self):
        return "action:{}, key{}\n".format(self.action, self.key)
        
class MouseRecoder():
    def __init__(self, event_queue):
        ''' q is event queue '''
        self.event_queue = event_queue
        # ...or, in a non-blocking fashion:
        self.listener = mouse.Listener(
            on_move=self.on_move,
            on_click=self.on_click,
            on_scroll=self.on_scroll,
            )
        # listener.start()
        # listener.join()

    def start(self):
        self.listener.start()

    def join(self):
        self.listener.join()

    def on_move(self, x, y):
        if stop_flag: return False
        logging.debug('Pointer moved to {0}'.format((x, y)))
        e = MouseEvent("move", x, y)
        self.event_queue.put(e)

    def on_click(self, x, y, button, pressed):
        if stop_flag: return False
        logging.debug('{0} at {1}'.format(
                'Pressed' if pressed else 'Released',
                (x, y)))
        e = MouseEvent("click", x, y, button, pressed)
        self.event_queue.put(e)
        if not pressed:
                # Stop listener
                return False

    def on_scroll(self, x, y, dx, dy):
        if stop_flag: 
            logging.info("scroll stop_flag")
            return False
        logging.debug('Scrolled {0} at {1}'.format(
                'down' if dy < 0 else 'up',
                (x, y)))
        e = MouseEvent("scroll", x, y)
        self.event_queue.put(e)



class KeyBoardRecoder():
    def __init__(self, event_queue):
        ''' q is event queue '''
        self.event_queue = event_queue
        self.listener = keyboard.Listener(
            on_press=self.on_press,
            on_release=self.on_release
            )

    def start(self):
        self.listener.start()

    def join(self):
        self.listener.join()

    def on_press(self, key):
        if stop_flag: 
            logging.info("press stop_flag")
            return False
        try:
            logging.debug('alphanumeric key {0} pressed'.format(key.char))
        except AttributeError:
            logging.debug('special key {0} pressed'.format(key))

        e = KeyBoardEvent("press", key)
        self.event_queue.put(e)

    def on_release(self, key):
        if stop_flag: 
            logging.info("release stop_flag")
            return False
        logging.debug('{0} released'.format(key))
        e = KeyBoardEvent("release", key)
        self.event_queue.put(e)
        if key == keyboard.Key.esc:
            # Stop listener
            return False


class Actions:
    def __init__(self):
        pass
    def load_data(self, filename):
        pass



class Recoder():
    def __init__(self):
        self.q = Queue()
        self.mouse_recoder = MouseRecoder(self.q)
        self.keyboard_recoder = KeyBoardRecoder(self.q)

    def start(self):
        self.mouse_recoder.start()
        self.keyboard_recoder.start()

    def phony_action(self):
        mouse_controller = mouse.Controller()
        mouse_controller.move(1, 1)
        keyboard_controller = keyboard.Controller()
        keyboard_controller.release(keyboard.Key.space)

    def stop(self):
        global stop_flag
        stop_flag = True
        self.phony_action()
        self.join()

    def join(self):
        self.mouse_recoder.join()
        self.keyboard_recoder.join()

    def show_queue(self):
        data = list(self.q.queue)
        return data

    def save_data(self, filename):
        event_data = list(self.q.queue)
        with open(filename, 'w') as fd:
            for event in event_data:
                line = event.dump()
                fd.write(line)


if __name__ == "__main__":
    recoder = Recoder()
    recoder.start()
    print("action size:", recoder.q.qsize())
    time.sleep(5) 
    print("action size:", recoder.q.qsize())
    recoder.stop()
    print("action size:", recoder.q.qsize()) 
    recoder.save_data("recoder_info.txt")