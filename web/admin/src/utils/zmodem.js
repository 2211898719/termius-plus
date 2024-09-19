/**
 *
 * Allow xterm.js to handle ZMODEM uploads and downloads.
 *
 * This addon is a wrapper around zmodem.js. It adds the following to the
 *  Terminal class:
 *
 * - function `zmodemAttach(<WebSocket>, <Object>)` - creates a Zmodem.Sentry
 *      on the passed WebSocket object. The Object passed is optional and
 *      can contain:
 *          - noTerminalWriteOutsideSession: Suppress writes from the Sentry
 *            object to the Terminal while there is no active Session. This
 *            is necessary for compatibility with, for example, the
 *            `attach.js` addon.
 *
 * - event `zmodemDetect` - fired on Zmodem.Sentry’s `on_detect` callback.
 *      Passes the zmodem.js Detection object.
 *
 * - event `zmodemRetract` - fired on Zmodem.Sentry’s `on_retract` callback.
 *
 * You’ll need to provide logic to handle uploads and downloads.
 * See zmodem.js’s documentation for more details.
 *
 * **IMPORTANT:** After you confirm() a zmodem.js Detection, if you have
 *  used the `attach` or `terminado` addons, you’ll need to suspend their
 *  operation for the duration of the ZMODEM session. (The demo does this
 *  via `detach()` and a re-`attach()`.)
 */
import ZmodemBrowser from 'nora-zmodemjs/src/zmodem_browser';
import { Terminal } from '@xterm/xterm';

Object.assign(Terminal.prototype, {
    zmodemAttach: function zmodemAttach(ws, opts) {
        const term = this;

        if (!opts) opts = {};

        const senderFunc = function _ws_sender_func(octets) {
            ws.send(new Uint8Array(octets));
        };

        let zsentry;

        function _shouldWrite() {
            return (
                !!zsentry.get_confirmed_session() ||
                !opts.noTerminalWriteOutsideSession
            );
        }

        zsentry = new ZmodemBrowser.Sentry({
            to_terminal: function _to_terminal(octets) {
                if (_shouldWrite()) {
                    term.write(String.fromCharCode.apply(String, octets));
                }
            },

            sender: senderFunc,

            on_retract: function _on_retract() {
                if (term.zmodemRetract) {
                    term.zmodemRetract();
                }
            },

            on_detect: function _on_detect(detection) {
                if (term.zmodemDetect) {
                    term.zmodemDetect(detection);
                }
            }
        });

        function handleWSMessage(evt) {
            if (typeof evt.data === 'string') {
                console.log(evt.data)
            } else {
                zsentry.consume(evt.data);
            }
        }

        ws.binaryType = 'arraybuffer';
        ws.addEventListener('message', handleWSMessage);
    },

    zmodemBrowser: ZmodemBrowser.Browser
});

export default Terminal;
