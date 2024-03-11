import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.tnd.Utils



class HostCardEmulatorService: HostApduService() {

    companion object {
        val TAG = "Host Card Emulator"
        val STATUS_SUCCESS = "9000"
        val STATUS_FAILED = "6F00"
        val CLA_NOT_SUPPORTED = "6E00"
        val INS_NOT_SUPPORTED = "6D00"
        val AID = "A0000002471011"
        val SELECT_INS = "A4"
        val DEFAULT_CLA = "00"
        val MIN_APDU_LENGTH = 12
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: " + reason)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            Log.d(TAG, "Received null APDU")
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        val hexCommandApdu = Utils.toHex(commandApdu)
        Log.d(TAG, "Received APDU: $hexCommandApdu")

        if (hexCommandApdu.length < MIN_APDU_LENGTH) {
            Log.d(TAG, "APDU length too short")
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        val cla = hexCommandApdu.substring(0, 2)
        val ins = hexCommandApdu.substring(2, 4)
        val p1 = hexCommandApdu.substring(4, 6)
        val p2 = hexCommandApdu.substring(6, 8)
        val lc = Integer.parseInt(hexCommandApdu.substring(8, 10), 16)
        val data = hexCommandApdu.substring(10, 10 + 2 * lc)

        Log.d(TAG, "Parsed APDU - CLA: $cla, INS: $ins, P1: $p1, P2: $p2, Lc: $lc, Data: $data")

        if (cla != DEFAULT_CLA || ins != SELECT_INS || p1 != "04" || p2 != "00" || data.uppercase() != AID) {
            Log.d(TAG, "Invalid command or AID")
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        Log.d(TAG, "Valid SELECT AID command received")
        return Utils.hexStringToByteArray(STATUS_SUCCESS)
    }

}
