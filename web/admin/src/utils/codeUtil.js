import chatWindowScollLock from "lodash/seq";

const States = {

    text: 0, // 文本状态

    codeStartSm: 1, // 小代码块状态

    codeStartBig: 2, // 大代码块状态

}


/**

 * 判断 markdown 文本中是否有未闭合的代码块

 * @param text

 * @returns {boolean}

 */

function isInCode(text) {
    let state = States.text
    let source = text
    let inStart = true // 是否处于文本开始状态，即还没有消费过文本

    while (source) { // 当文本被解析消费完后，就是个空字符串了，就能跳出循环
        let char = source.charAt(0) // 取第 0 个字
        switch (state) {
            case States.text:
                if (/^\n?```/.test(source)) {
                    // 以 ``` 或者 \n``` 开头。表示大代码块开始。
                    // 一般情况下，代码块前面都需要换行。但是如果是在文本的开头，就不需要换行。
                    if (inStart || source.startsWith("\n")) {
                        state = States.codeStartBig
                    }
                    source = source.replace(/^\n?```/, "")
                } else if (char === "\\") {
                    // 遇到转义符，跳过下一个字符
                    source = source.slice(2)
                } else if (char === "`") {
                    // 以 ` 开头。表示小代码块开始。
                    state = States.codeStartSm
                    source = source.slice(1)
                } else {
                    // 其他情况，直接消费当前字符
                    source = source.slice(1)
                }
                inStart = false
                break
            case States.codeStartSm:
                if (char === "`") {
                    // 遇到第二个 `，表示代码块结束
                    state = States.text
                    source = source.slice(1)
                } else if (char === "\\") {
                    // 遇到转义符，跳过下一个字符
                    source = source.slice(2)
                } else {
                    // 其他情况，直接消费当前字符
                    source = source.slice(1)
                }
                break
            case States.codeStartBig:
                if (/^\n```/.test(source)) {
                    // 遇到第二个 ```，表示代码块结束
                    state = States.text
                    source = source.replace(/^\n```/, "")
                } else {
                    // 其他情况，直接消费当前字符
                    source = source.slice(1)
                }
                break
        }
    }

    return state !== States.text
}


export default isInCode

export class Pipe {
    str = ''
    timer = 0
    target = () => {
    }

    reset() {
        this.str = ''
        cancelAnimationFrame(this.timer)
        this.target = () => {
        }
    }

    start(cell) {
        this.target = cell


        const recursiveTimeoutFunction = () => requestAnimationFrame(() => {
            this.consume(this.getFirstStr())
            this.pop()
            console.log(1)
            this.timer = recursiveTimeoutFunction()
        })

        this.timer = recursiveTimeoutFunction()
    }

    write(chunk) {
        this.str += chunk
    }

    getFirstStr() {
        return this.str[0]
    }

    pop() {
        this.str = this.str.substring(1)
    }

    consume(message) {
        const ans = this.target
        if (message) {
            ans(message)
        }
    }

    consumeAll() {
        this.consume(this.str)
        this.str = ''
        cancelAnimationFrame(this.timer)
    }
}

export function findLastNonEmptyTextNode(node) {
    // 定义一个辅助函数来递归查找
    function traverse(node) {
        if (node.nodeType === Node.TEXT_NODE) {
            // 去除首尾空格和换行符，并检查内容是否为空
            const trimmedText = node.nodeValue?.trim().replace(/\n/g, '');
            if (trimmedText) {
                return node.parentNode;
            }
        } else if (node.childNodes.length > 0) {
            for (let i = node.childNodes.length - 1; i >= 0; i--) {
                const result = traverse(node.childNodes[i]);
                if (result) {
                    return result;
                }
            }
        }
        return null;
    }

    return traverse(node);
}