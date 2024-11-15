import smCrypto from "sm-crypto";

/**
 * 用于加密数据
 * @param {any} msgString - 需加密的数据
 * @param {string} publicKey - 加密公钥
 * @return {string} 加密后的字符串
 */
export function doEncrypt(msgString, publicKey) {
  let msg = msgString;
  if (typeof (msgString) !== "string") {
    msg = JSON.stringify(msgString);
  }
  let sm2 = smCrypto.sm2;
  // 1 - C1C3C2；	0 - C1C2C3；	默认为1
  let cipherMode = 1; // 特别注意,此处前后端需保持一致
  // 加密结果
  let encryptData = sm2.doEncrypt(msg, publicKey, cipherMode);
  // 加密后的密文前需要添加04，后端才能正常解密
  return "04" + encryptData;
}

/**
 * 用于解密数据
 * @param {any} enStr - 待解密的数据
 * @param {string} privateKey - 解密私钥
 * @return {string} 解密后的字符串
 */
export function doDecryptStr(enStr, privateKey) {
  let msg = enStr;
  if (typeof (enStr) !== "string") {
    msg = JSON.stringify(enStr);
  }
  let sm2 = smCrypto.sm2;
  // 1 - C1C3C2；	0 - C1C2C3；	默认为1
  let cipherMode = 1;
  // 加密后的密文，需要前去掉04。因为doDecrypt中自行添加了04，后端加密代码也自行添加了04
  let en = msg.slice(2);
  // 解密结果
  let d = sm2.doDecrypt(en, privateKey, cipherMode);
  return d;
}
