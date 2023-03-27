package one.mixin.android.ui.transfer

import kotlin.random.Random

class RandomStringUtil {
    companion object {
        private const val ALPHA_NUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        /**
         * 生成指定长度的随机字符串，由数字和字母组成。
         *
         * @param length 生成字符串的长度
         * @return 随机字符串
         */
        fun generateRandomString(length: Int): String {
            return buildString {
                repeat(length) {
                    val randomIndex = Random.nextInt(0, ALPHA_NUMERIC_CHARS.length)
                    append(ALPHA_NUMERIC_CHARS[randomIndex])
                }
            }
        }
    }
}
