package com.anonimbiri.removedpi.data

data class DpiSettings(
    val bufferSize: Int = 32768,
    val tcpFastOpen: Boolean = false,
    val enableTcpNodelay: Boolean = true,
    
    val appTheme: AppTheme = AppTheme.AMOLED,
    
    val whitelist: Set<String> = setOf(
        "turkiye.gov.tr", "giris.turkiye.gov.tr",
        "cimer.gov.tr", "resmigazete.gov.tr", "tccb.gov.tr", "tbmm.gov.tr",
        "anayasa.gov.tr", "yargitay.gov.tr", "danistay.gov.tr", "sayistay.gov.tr",
        "ysk.gov.tr", "icisleri.gov.tr", "adalet.gov.tr", "uyap.gov.tr",
        "nvi.gov.tr", "egm.gov.tr", "jandarma.gov.tr", "msb.gov.tr",
        "disisleri.gov.tr", "meb.gov.tr", "eba.gov.tr", "saglik.gov.tr",
        "enabiz.gov.tr", "mhrs.gov.tr", "aile.gov.tr", "csgb.gov.tr",
        "sgk.gov.tr", "goc.gov.tr", "afad.gov.tr", "mgm.gov.tr",
        "tarimorman.gov.tr", "uab.gov.tr", "ticaret.gov.tr", "sanayi.gov.tr",
        "hmb.gov.tr", "gib.gov.tr", "ivd.gib.gov.tr", "tcmb.gov.tr",
        "bddk.org.tr", "spk.gov.tr", "tbb.org.tr", "kgk.gov.tr",
        "iskur.gov.tr", "tse.org.tr", "turkpatent.gov.tr", "tubitak.gov.tr",
        "aselsan.com.tr", "tusas.com", "roketsan.com.tr", "havelsan.com.tr",
        "stm.com.tr",
        "ziraatbank.com.tr", "ziraat.com.tr", "vakifbank.com.tr", "halkbank.com.tr",
        "isbank.com.tr", "iscep.com.tr", "garanti.com.tr", "garantibbva.com.tr",
        "yapikredi.com.tr", "akbank.com", "akbank.com.tr",
        "qnbfinansbank.com", "qnb.com.tr", "denizbank.com", "teb.com.tr",
        "kuveytturk.com.tr", "albarakaturk.com.tr", "turkiyefinans.com.tr",
        "ziraatkatilim.com.tr", "vakifkatilim.com.tr", "emlakkatilim.com.tr",
        "ing.com.tr", "hsbc.com.tr", "odeabank.com.tr", "fibabanka.com.tr",
        "sekerbank.com.tr", "bkm.com.tr", "tobb.org.tr", "ito.org.tr",
        "ato.org.tr", "iso.org.tr", "yok.gov.tr", "osym.gov.tr", "ais.osym.gov.tr",
        "anadolu.edu.tr", "metu.edu.tr", "odtu.edu.tr", "itu.edu.tr",
        "yildiz.edu.tr", "istanbul.edu.tr", "hacettepe.edu.tr", "gazi.edu.tr",
        "bogazici.edu.tr", "marmara.edu.tr", "ege.edu.tr", "deu.edu.tr",
        "ankara.edu.tr", "bilkent.edu.tr", "koc.edu.tr", "sabanciuniv.edu",
        "dergipark.org.tr", "ptt.gov.tr", "pttavm.com", "kgm.gov.tr",
        "tcdd.gov.tr", "tcddtasimacilik.gov.tr", "thy.com", "turkishairlines.com",
        "anadolujet.com", "dhmi.gov.tr", "shgm.gov.tr", "btk.gov.tr",
        "turksat.com.tr", "turktelekom.com.tr", "ttnet.com.tr",
        "turkcell.com.tr", "superonline.net", "vodafone.com.tr", "kablonet.com.tr",
        "millenicom.com.tr", "turk.net", "epdk.gov.tr", "enerjisa.com.tr",
        "ckbogazici.com.tr", "bedas.com.tr", "ayedas.com.tr", "toroslar.com.tr",
        "baskent.com.tr", "iski.istanbul", "igdas.istanbul", "aski.gov.tr",
        "izsu.gov.tr", "buski.gov.tr", "diski.gov.tr", "gaski.gov.tr",
        "koski.gov.tr", "istanbul.bel.tr", "ankara.bel.tr", "izmir.bel.tr",
        "bursa.bel.tr", "antalya.bel.tr", "adana.bel.tr", "gaziantep.bel.tr",
        "konya.bel.tr", "kayseri.bel.tr", "kizilay.org.tr", "yesilay.org.tr",
        "barobirlik.org.tr", "istanbulbarosu.org.tr", "ankarabarosu.org.tr",
        "aa.com.tr", "trt.net.tr", "trthaber.com", "trtizle.com", "basinhaber.gov.tr"
    ),

    val desyncMethod: DesyncMethod = DesyncMethod.SPLIT,
    val desyncHttp: Boolean = true,
    val desyncHttps: Boolean = true,
    val firstPacketSize: Int = 1,
    val splitDelay: Long = 2L,
    val mixHostCase: Boolean = true,
    val splitCount: Int = 3,
    val ttlValue: Int = 3,
    val autoTtl: Boolean = false,
    val minTtl: Int = 3,
    val fakePacketMode: FakePacketMode = FakePacketMode.NONE,
    val blockQuic: Boolean = true,
    val enableLogs: Boolean = true
)

enum class DesyncMethod { 
    SPLIT,
    DISORDER,
    FRAG_BY_SNI,
    TTL,
    SNI_SPLIT
}

enum class FakePacketMode {
    NONE,
    WRONG_SEQ,
    WRONG_CHKSUM,
    BOTH
}

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
enum class AppTheme { SYSTEM, AMOLED, ANIME }