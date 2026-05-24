package ohi.andre.consolelauncher.managers.flashlight

class Constants private constructor() {
    companion object {
        const val ID_DEVICE = "0"

        const val ID_DEVICE_OUTPUT = "1"
        const val ID_DEVICE_OUTPUT_TORCH = "10"
        const val ID_DEVICE_OUTPUT_TORCH_FLASH = "11"
        const val ID_DEVICE_OUTPUT_TORCH_FLASH_LEGACY = "12"
        const val ID_DEVICE_OUTPUT_TORCH_FLASH_NEW = "13"
        const val ID_DEVICE_OUTPUT_TORCH_SCREEN = "14"
        const val ID_DEVICE_OUTPUT_VIBRATOR = "15"

        const val ID_DEVICE_INPUT = "2"
        const val ID_DEVICE_INPUT_VOLUMEKEY = "20"
        const val ID_DEVICE_INPUT_VOLUMEKEY_NATIVE = "21"
        const val ID_DEVICE_INPUT_VOLUMEKEY_ROCKER = "22"
        const val ID_DEVICE_INPUT_PROXIMITY = "23"

        const val PLAY_URI = "https://play.google.com/store/apps/details?id=in.blogspot.anselmbros.torchie"
        const val WEB_URI = "https://torchieapp.wordpress.com"
        const val ABOUTANSELM_URI = "http://anselmbros.blogspot.in/p/about-us.html"
        const val COMMUNITY_URI = "https://plus.google.com/communities/114100054385968340083"
        const val FACEBOOK_URI = "https://facebook.com/torchieapp"
        const val GOOGLEPLUS_URI = "https://plus.google.com/111668132285982978436"
        const val WEB_DONATE_URI = "https://torchieapp.wordpress.com/donate/"
    }
}
