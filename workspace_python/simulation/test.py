from pynput import mouse, keyboard
import functools
from queue import Queue
import logging


class MouseEvent():
    def __init__(self, action, x, y, button=None, pressed=None, time=None):
        self.action = action
        self.x = x
        self.y = y

class KeyBoardEvent():
    def __init__(self, key, time=None):
        self.key = key

        
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
        logging.debug('Pointer moved to {0}'.format((x, y)))
        e = MouseEvent("move", x, y)
        self.event_queue.put(e)

    def on_click(self, x, y, button, pressed):
        logging.debug('{0} at {1}'.format(
                'Pressed' if pressed else 'Released',
                (x, y)))
        e = MouseEvent("click", x, y, button, pressed)
        self.event_queue.put(e)
        if not pressed:
                # Stop listener
                return False

    def on_scroll(self, x, y, dx, dy):
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
        try:
            logging.debug('alphanumeric key {0} pressed'.format(key.char))
            e = KeyBoardEvent(key)
            self.event_queue.put(e)
        except AttributeError:
            logging.debug('special key {0} pressed'.format(key))

        e = KeyBoardEvent(key)
        self.event_queue.put(e)

    def on_release(self, key):
        logging.debug('{0} released'.format(key))
        e = KeyBoardEvent(key)
        self.event_queue.put(e)
        if key == keyboard.Key.esc:
            # Stop listener
            return False


#class Actions():


class Recoder():
    def __init__(self):
        self.q = Queue()
        self.mouse_recoder = MouseRecoder(self.q)
        self.keyboard_recoder = KeyBoardRecoder(self.q)

    def start(self):
        self.mouse_recoder.start()
        self.keyboard_recoder.start()

    def join(self):
        self.mouse_recoder.join()
        self.keyboard_recoder.join()

    def show_queue(self):
        data = list(self.q)
        return data

#class Executer():
