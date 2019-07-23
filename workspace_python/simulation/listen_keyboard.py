from pynput import mouse, keyboard
import functools
from queue import Queue




class MouseEvent():
    def __init__(self, x, y):
        self.x = x
        self.y = y

        
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
            print('alphanumeric key {0} pressed'.format(key.char))
        except AttributeError:
            print('special key {0} pressed'.format(key))

    def on_release(self, key):
        print('{0} released'.format(key))
        if key == keyboard.Key.esc:
            # Stop listener
            return False

if __name__ == "__main__":
    q = Queue()
    keyboard_recoder = KeyBoardRecoder(q)
    keyboard_recoder.start()
    keyboard_recoder.join()