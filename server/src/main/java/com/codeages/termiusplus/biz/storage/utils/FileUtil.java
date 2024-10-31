package com.codeages.termiusplus.biz.storage.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.storage.entity.File;
import com.codeages.termiusplus.biz.storage.vo.FileDto;
import com.codeages.termiusplus.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author hjl
 */
@Slf4j
public class FileUtil {
    private static Map<String, String> FILE_EXT_FILE_TYPE_MAP;

    static {
        FILE_EXT_FILE_TYPE_MAP = new HashMap<>(772) {{
            put("ez", "application/andrew-inset");
            put("aw", "application/applixware");
            put("atom", "application/atom+xml");
            put("atomcat", "application/atomcat+xml");
            put("atomsvc", "application/atomsvc+xml");
            put("ccxml", "application/ccxml+xml");
            put("cdmia", "application/cdmi-capability");
            put("cdmic", "application/cdmi-container");
            put("cdmid", "application/cdmi-domain");
            put("cdmio", "application/cdmi-object");
            put("cdmiq", "application/cdmi-queue");
            put("cu", "application/cu-seeme");
            put("davmount", "application/davmount+xml");
            put("dbk", "application/docbook+xml");
            put("dssc", "application/dssc+der");
            put("xdssc", "application/dssc+xml");
            put("ecma", "application/ecmascript");
            put("emma", "application/emma+xml");
            put("epub", "application/epub+zip");
            put("exi", "application/exi");
            put("pfr", "application/font-tdpfr");
            put("gml", "application/gml+xml");
            put("gpx", "application/gpx+xml");
            put("gxf", "application/gxf");
            put("stk", "application/hyperstudio");
            put("ink", "application/inkml+xml");
            put("ipfix", "application/ipfix");
            put("jar", "application/java-archive");
            put("ser", "application/java-serialized-object");
            put("class", "application/java-vm");
            put("js", "application/javascript");
            put("json", "application/json");
            put("jsonml", "application/jsonml+json");
            put("lostxml", "application/lost+xml");
            put("hqx", "application/mac-binhex40");
            put("cpt", "application/mac-compactpro");
            put("mads", "application/mads+xml");
            put("mrc", "application/marc");
            put("mrcx", "application/marcxml+xml");
            put("ma", "application/mathematica");
            put("mathml", "application/mathml+xml");
            put("mbox", "application/mbox");
            put("mscml", "application/mediaservercontrol+xml");
            put("metalink", "application/metalink+xml");
            put("meta4", "application/metalink4+xml");
            put("mets", "application/mets+xml");
            put("mods", "application/mods+xml");
            put("m21", "application/mp21");
            put("mp4s", "application/mp4");
            put("doc", "application/msword");
            put("mxf", "application/mxf");
            put("bin", "application/octet-stream");
            put("oda", "application/oda");
            put("opf", "application/oebps-package+xml");
            put("ogx", "application/ogg");
            put("omdoc", "application/omdoc+xml");
            put("onetoc", "application/onenote");
            put("oxps", "application/oxps");
            put("xer", "application/patch-ops-error+xml");
            put("pdf", "application/pdf");
            put("pgp", "application/pgp-encrypted");
            put("asc", "application/pgp-signature");
            put("prf", "application/pics-rules");
            put("p10", "application/pkcs10");
            put("p7m", "application/pkcs7-mime");
            put("p7s", "application/pkcs7-signature");
            put("p8", "application/pkcs8");
            put("ac", "application/pkix-attr-cert");
            put("cer", "application/pkix-cert");
            put("crl", "application/pkix-crl");
            put("pkipath", "application/pkix-pkipath");
            put("pki", "application/pkixcmp");
            put("pls", "application/pls+xml");
            put("ai", "application/postscript");
            put("cww", "application/prs.cww");
            put("pskcxml", "application/pskc+xml");
            put("rdf", "application/rdf+xml");
            put("rif", "application/reginfo+xml");
            put("rnc", "application/relax-ng-compact-syntax");
            put("rl", "application/resource-lists+xml");
            put("rld", "application/resource-lists-diff+xml");
            put("rs", "application/rls-services+xml");
            put("gbr", "application/rpki-ghostbusters");
            put("mft", "application/rpki-manifest");
            put("roa", "application/rpki-roa");
            put("rsd", "application/rsd+xml");
            put("rss", "application/rss+xml");
            put("rtf", "application/rtf");
            put("sbml", "application/sbml+xml");
            put("scq", "application/scvp-cv-request");
            put("scs", "application/scvp-cv-response");
            put("spq", "application/scvp-vp-request");
            put("spp", "application/scvp-vp-response");
            put("sdp", "application/sdp");
            put("setpay", "application/set-payment-initiation");
            put("setreg", "application/set-registration-initiation");
            put("shf", "application/shf+xml");
            put("smi", "application/smil+xml");
            put("rq", "application/sparql-query");
            put("srx", "application/sparql-results+xml");
            put("gram", "application/srgs");
            put("grxml", "application/srgs+xml");
            put("sru", "application/sru+xml");
            put("ssdl", "application/ssdl+xml");
            put("ssml", "application/ssml+xml");
            put("tei", "application/tei+xml");
            put("tfi", "application/thraud+xml");
            put("tsd", "application/timestamped-data");
            put("plb", "application/vnd.3gpp.pic-bw-large");
            put("psb", "application/vnd.3gpp.pic-bw-small");
            put("pvb", "application/vnd.3gpp.pic-bw-var");
            put("tcap", "application/vnd.3gpp2.tcap");
            put("pwn", "application/vnd.3m.post-it-notes");
            put("aso", "application/vnd.accpac.simply.aso");
            put("imp", "application/vnd.accpac.simply.imp");
            put("acu", "application/vnd.acucobol");
            put("atc", "application/vnd.acucorp");
            put("air", "application/vnd.adobe.air-application-installer-package+zip");
            put("fcdt", "application/vnd.adobe.formscentral.fcdt");
            put("fxp", "application/vnd.adobe.fxp");
            put("xdp", "application/vnd.adobe.xdp+xml");
            put("xfdf", "application/vnd.adobe.xfdf");
            put("ahead", "application/vnd.ahead.space");
            put("azf", "application/vnd.airzip.filesecure.azf");
            put("azs", "application/vnd.airzip.filesecure.azs");
            put("azw", "application/vnd.amazon.ebook");
            put("acc", "application/vnd.americandynamics.acc");
            put("ami", "application/vnd.amiga.ami");
            put("apk", "application/vnd.android.package-archive");
            put("cii", "application/vnd.anser-web-certificate-issue-initiation");
            put("fti", "application/vnd.anser-web-funds-transfer-initiation");
            put("atx", "application/vnd.antix.game-component");
            put("mpkg", "application/vnd.apple.installer+xml");
            put("m3u8", "application/vnd.apple.mpegurl");
            put("swi", "application/vnd.aristanetworks.swi");
            put("iota", "application/vnd.astraea-software.iota");
            put("aep", "application/vnd.audiograph");
            put("mpm", "application/vnd.blueice.multipass");
            put("bmi", "application/vnd.bmi");
            put("rep", "application/vnd.businessobjects");
            put("cdxml", "application/vnd.chemdraw+xml");
            put("mmd", "application/vnd.chipnuts.karaoke-mmd");
            put("cdy", "application/vnd.cinderella");
            put("cla", "application/vnd.claymore");
            put("rp9", "application/vnd.cloanto.rp9");
            put("c4g", "application/vnd.clonk.c4group");
            put("c11amc", "application/vnd.cluetrust.cartomobile-config");
            put("c11amz", "application/vnd.cluetrust.cartomobile-config-pkg");
            put("csp", "application/vnd.commonspace");
            put("cdbcmsg", "application/vnd.contact.cmsg");
            put("cmc", "application/vnd.cosmocaller");
            put("clkx", "application/vnd.crick.clicker");
            put("clkk", "application/vnd.crick.clicker.keyboard");
            put("clkp", "application/vnd.crick.clicker.palette");
            put("clkt", "application/vnd.crick.clicker.template");
            put("clkw", "application/vnd.crick.clicker.wordbank");
            put("wbs", "application/vnd.criticaltools.wbs+xml");
            put("pml", "application/vnd.ctc-posml");
            put("ppd", "application/vnd.cups-ppd");
            put("car", "application/vnd.curl.car");
            put("pcurl", "application/vnd.curl.pcurl");
            put("dart", "application/vnd.dart");
            put("rdz", "application/vnd.data-vision.rdz");
            put("uvf", "application/vnd.dece.data");
            put("uvt", "application/vnd.dece.ttml+xml");
            put("uvx", "application/vnd.dece.unspecified");
            put("uvz", "application/vnd.dece.zip");
            put("fe_launch", "application/vnd.denovo.fcselayout-link");
            put("dna", "application/vnd.dna");
            put("mlp", "application/vnd.dolby.mlp");
            put("dpg", "application/vnd.dpgraph");
            put("dfac", "application/vnd.dreamfactory");
            put("kpxx", "application/vnd.ds-keypoint");
            put("ait", "application/vnd.dvb.ait");
            put("svc", "application/vnd.dvb.service");
            put("geo", "application/vnd.dynageo");
            put("mag", "application/vnd.ecowin.chart");
            put("nml", "application/vnd.enliven");
            put("esf", "application/vnd.epson.esf");
            put("msf", "application/vnd.epson.msf");
            put("qam", "application/vnd.epson.quickanime");
            put("slt", "application/vnd.epson.salt");
            put("ssf", "application/vnd.epson.ssf");
            put("es3", "application/vnd.eszigno3+xml");
            put("ez2", "application/vnd.ezpix-album");
            put("ez3", "application/vnd.ezpix-package");
            put("fdf", "application/vnd.fdf");
            put("mseed", "application/vnd.fdsn.mseed");
            put("seed", "application/vnd.fdsn.seed");
            put("gph", "application/vnd.flographit");
            put("ftc", "application/vnd.fluxtime.clip");
            put("fm", "application/vnd.framemaker");
            put("fnc", "application/vnd.frogans.fnc");
            put("ltf", "application/vnd.frogans.ltf");
            put("fsc", "application/vnd.fsc.weblaunch");
            put("oas", "application/vnd.fujitsu.oasys");
            put("oa2", "application/vnd.fujitsu.oasys2");
            put("oa3", "application/vnd.fujitsu.oasys3");
            put("fg5", "application/vnd.fujitsu.oasysgp");
            put("bh2", "application/vnd.fujitsu.oasysprs");
            put("ddd", "application/vnd.fujixerox.ddd");
            put("xdw", "application/vnd.fujixerox.docuworks");
            put("xbd", "application/vnd.fujixerox.docuworks.binder");
            put("fzs", "application/vnd.fuzzysheet");
            put("txd", "application/vnd.genomatix.tuxedo");
            put("ggb", "application/vnd.geogebra.file");
            put("ggt", "application/vnd.geogebra.tool");
            put("gex", "application/vnd.geometry-explorer");
            put("gxt", "application/vnd.geonext");
            put("g2w", "application/vnd.geoplan");
            put("g3w", "application/vnd.geospace");
            put("gmx", "application/vnd.gmx");
            put("kml", "application/vnd.google-earth.kml+xml");
            put("kmz", "application/vnd.google-earth.kmz");
            put("gqf", "application/vnd.grafeq");
            put("gac", "application/vnd.groove-account");
            put("ghf", "application/vnd.groove-help");
            put("gim", "application/vnd.groove-identity-message");
            put("grv", "application/vnd.groove-injector");
            put("gtm", "application/vnd.groove-tool-message");
            put("tpl", "application/vnd.groove-tool-template");
            put("vcg", "application/vnd.groove-vcard");
            put("hal", "application/vnd.hal+xml");
            put("zmm", "application/vnd.handheld-entertainment+xml");
            put("hbci", "application/vnd.hbci");
            put("les", "application/vnd.hhe.lesson-player");
            put("hpgl", "application/vnd.hp-hpgl");
            put("hpid", "application/vnd.hp-hpid");
            put("hps", "application/vnd.hp-hps");
            put("jlt", "application/vnd.hp-jlyt");
            put("pcl", "application/vnd.hp-pcl");
            put("pclxl", "application/vnd.hp-pclxl");
            put("sfd-hdstx", "application/vnd.hydrostatix.sof-data");
            put("mpy", "application/vnd.ibm.minipay");
            put("afp", "application/vnd.ibm.modcap");
            put("irm", "application/vnd.ibm.rights-management");
            put("sc", "application/vnd.ibm.secure-container");
            put("icc", "application/vnd.iccprofile");
            put("igl", "application/vnd.igloader");
            put("ivp", "application/vnd.immervision-ivp");
            put("ivu", "application/vnd.immervision-ivu");
            put("igm", "application/vnd.insors.igm");
            put("xpw", "application/vnd.intercon.formnet");
            put("i2g", "application/vnd.intergeo");
            put("qbo", "application/vnd.intu.qbo");
            put("qfx", "application/vnd.intu.qfx");
            put("rcprofile", "application/vnd.ipunplugged.rcprofile");
            put("irp", "application/vnd.irepository.package+xml");
            put("xpr", "application/vnd.is-xpr");
            put("fcs", "application/vnd.isac.fcs");
            put("jam", "application/vnd.jam");
            put("rms", "application/vnd.jcp.javame.midlet-rms");
            put("jisp", "application/vnd.jisp");
            put("joda", "application/vnd.joost.joda-archive");
            put("ktz", "application/vnd.kahootz");
            put("karbon", "application/vnd.kde.karbon");
            put("chrt", "application/vnd.kde.kchart");
            put("kfo", "application/vnd.kde.kformula");
            put("flw", "application/vnd.kde.kivio");
            put("kon", "application/vnd.kde.kontour");
            put("kpr", "application/vnd.kde.kpresenter");
            put("ksp", "application/vnd.kde.kspread");
            put("kwd", "application/vnd.kde.kword");
            put("htke", "application/vnd.kenameaapp");
            put("kia", "application/vnd.kidspiration");
            put("kne", "application/vnd.kinar");
            put("skp", "application/vnd.koan");
            put("sse", "application/vnd.kodak-descriptor");
            put("lasxml", "application/vnd.las.las+xml");
            put("lbd", "application/vnd.llamagraphics.life-balance.desktop");
            put("lbe", "application/vnd.llamagraphics.life-balance.exchange+xml");
            put("123", "application/vnd.lotus-1-2-3");
            put("apr", "application/vnd.lotus-approach");
            put("pre", "application/vnd.lotus-freelance");
            put("nsf", "application/vnd.lotus-notes");
            put("org", "application/vnd.lotus-organizer");
            put("scm", "application/vnd.lotus-screencam");
            put("lwp", "application/vnd.lotus-wordpro");
            put("portpkg", "application/vnd.macports.portpkg");
            put("mcd", "application/vnd.mcd");
            put("mc1", "application/vnd.medcalcdata");
            put("cdkey", "application/vnd.mediastation.cdkey");
            put("mwf", "application/vnd.mfer");
            put("mfm", "application/vnd.mfmp");
            put("flo", "application/vnd.micrografx.flo");
            put("igx", "application/vnd.micrografx.igx");
            put("mif", "application/vnd.mif");
            put("daf", "application/vnd.mobius.daf");
            put("dis", "application/vnd.mobius.dis");
            put("mbk", "application/vnd.mobius.mbk");
            put("mqy", "application/vnd.mobius.mqy");
            put("msl", "application/vnd.mobius.msl");
            put("plc", "application/vnd.mobius.plc");
            put("txf", "application/vnd.mobius.txf");
            put("mpn", "application/vnd.mophun.application");
            put("mpc", "application/vnd.mophun.certificate");
            put("xul", "application/vnd.mozilla.xul+xml");
            put("cil", "application/vnd.ms-artgalry");
            put("cab", "application/vnd.ms-cab-compressed");
            put("xls", "application/vnd.ms-excel");
            put("xlam", "application/vnd.ms-excel.addin.macroenabled.12");
            put("xlsb", "application/vnd.ms-excel.sheet.binary.macroenabled.12");
            put("xlsm", "application/vnd.ms-excel.sheet.macroenabled.12");
            put("xltm", "application/vnd.ms-excel.template.macroenabled.12");
            put("eot", "application/vnd.ms-fontobject");
            put("chm", "application/vnd.ms-htmlhelp");
            put("ims", "application/vnd.ms-ims");
            put("lrm", "application/vnd.ms-lrm");
            put("thmx", "application/vnd.ms-officetheme");
            put("cat", "application/vnd.ms-pki.seccat");
            put("stl", "application/vnd.ms-pki.stl");
            put("ppt", "application/vnd.ms-powerpoint");
            put("ppam", "application/vnd.ms-powerpoint.addin.macroenabled.12");
            put("pptm", "application/vnd.ms-powerpoint.presentation.macroenabled.12");
            put("sldm", "application/vnd.ms-powerpoint.slide.macroenabled.12");
            put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroenabled.12");
            put("potm", "application/vnd.ms-powerpoint.template.macroenabled.12");
            put("mpp", "application/vnd.ms-project");
            put("docm", "application/vnd.ms-word.document.macroenabled.12");
            put("dotm", "application/vnd.ms-word.template.macroenabled.12");
            put("wps", "application/vnd.ms-works");
            put("wpl", "application/vnd.ms-wpl");
            put("xps", "application/vnd.ms-xpsdocument");
            put("mseq", "application/vnd.mseq");
            put("mus", "application/vnd.musician");
            put("msty", "application/vnd.muvee.style");
            put("taglet", "application/vnd.mynfc");
            put("nlu", "application/vnd.neurolanguage.nlu");
            put("ntf", "application/vnd.nitf");
            put("nnd", "application/vnd.noblenet-directory");
            put("nns", "application/vnd.noblenet-sealer");
            put("nnw", "application/vnd.noblenet-web");
            put("ngdat", "application/vnd.nokia.n-gage.data");
            put("n-gage", "application/vnd.nokia.n-gage.symbian.install");
            put("rpst", "application/vnd.nokia.radio-preset");
            put("rpss", "application/vnd.nokia.radio-presets");
            put("edm", "application/vnd.novadigm.edm");
            put("edx", "application/vnd.novadigm.edx");
            put("ext", "application/vnd.novadigm.ext");
            put("odc", "application/vnd.oasis.opendocument.chart");
            put("otc", "application/vnd.oasis.opendocument.chart-template");
            put("odb", "application/vnd.oasis.opendocument.database");
            put("odf", "application/vnd.oasis.opendocument.formula");
            put("odft", "application/vnd.oasis.opendocument.formula-template");
            put("odg", "application/vnd.oasis.opendocument.graphics");
            put("otg", "application/vnd.oasis.opendocument.graphics-template");
            put("odi", "application/vnd.oasis.opendocument.image");
            put("oti", "application/vnd.oasis.opendocument.image-template");
            put("odp", "application/vnd.oasis.opendocument.presentation");
            put("otp", "application/vnd.oasis.opendocument.presentation-template");
            put("ods", "application/vnd.oasis.opendocument.spreadsheet");
            put("ots", "application/vnd.oasis.opendocument.spreadsheet-template");
            put("odt", "application/vnd.oasis.opendocument.text");
            put("odm", "application/vnd.oasis.opendocument.text-master");
            put("ott", "application/vnd.oasis.opendocument.text-template");
            put("oth", "application/vnd.oasis.opendocument.text-web");
            put("xo", "application/vnd.olpc-sugar");
            put("dd2", "application/vnd.oma.dd2+xml");
            put("oxt", "application/vnd.openofficeorg.extension");
            put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            put("sldx", "application/vnd.openxmlformats-officedocument.presentationml.slide");
            put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
            put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
            put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
            put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
            put("mgp", "application/vnd.osgeo.mapguide.package");
            put("dp", "application/vnd.osgi.dp");
            put("esa", "application/vnd.osgi.subsystem");
            put("pdb", "application/vnd.palm");
            put("paw", "application/vnd.pawaafile");
            put("str", "application/vnd.pg.format");
            put("ei6", "application/vnd.pg.osasli");
            put("efif", "application/vnd.picsel");
            put("wg", "application/vnd.pmi.widget");
            put("plf", "application/vnd.pocketlearn");
            put("pbd", "application/vnd.powerbuilder6");
            put("box", "application/vnd.previewsystems.box");
            put("mgz", "application/vnd.proteus.magazine");
            put("qps", "application/vnd.publishare-delta-tree");
            put("ptid", "application/vnd.pvi.ptid1");
            put("qxd", "application/vnd.quark.quarkxpress");
            put("bed", "application/vnd.realvnc.bed");
            put("mxl", "application/vnd.recordare.musicxml");
            put("musicxml", "application/vnd.recordare.musicxml+xml");
            put("cryptonote", "application/vnd.rig.cryptonote");
            put("cod", "application/vnd.rim.cod");
            put("rm", "application/vnd.rn-realmedia");
            put("rmvb", "application/vnd.rn-realmedia-vbr");
            put("link66", "application/vnd.route66.link66+xml");
            put("st", "application/vnd.sailingtracker.track");
            put("see", "application/vnd.seemail");
            put("sema", "application/vnd.sema");
            put("semd", "application/vnd.semd");
            put("semf", "application/vnd.semf");
            put("ifm", "application/vnd.shana.informed.formdata");
            put("itp", "application/vnd.shana.informed.formtemplate");
            put("iif", "application/vnd.shana.informed.interchange");
            put("ipk", "application/vnd.shana.informed.package");
            put("twd", "application/vnd.simtech-mindmapper");
            put("mmf", "application/vnd.smaf");
            put("teacher", "application/vnd.smart.teacher");
            put("sdkm", "application/vnd.solent.sdkm+xml");
            put("dxp", "application/vnd.spotfire.dxp");
            put("sfs", "application/vnd.spotfire.sfs");
            put("sdc", "application/vnd.stardivision.calc");
            put("sda", "application/vnd.stardivision.draw");
            put("sdd", "application/vnd.stardivision.impress");
            put("smf", "application/vnd.stardivision.math");
            put("sdw", "application/vnd.stardivision.writer");
            put("sgl", "application/vnd.stardivision.writer-global");
            put("smzip", "application/vnd.stepmania.package");
            put("sm", "application/vnd.stepmania.stepchart");
            put("sxc", "application/vnd.sun.xml.calc");
            put("stc", "application/vnd.sun.xml.calc.template");
            put("sxd", "application/vnd.sun.xml.draw");
            put("std", "application/vnd.sun.xml.draw.template");
            put("sxi", "application/vnd.sun.xml.impress");
            put("sti", "application/vnd.sun.xml.impress.template");
            put("sxm", "application/vnd.sun.xml.math");
            put("sxw", "application/vnd.sun.xml.writer");
            put("sxg", "application/vnd.sun.xml.writer.global");
            put("stw", "application/vnd.sun.xml.writer.template");
            put("sus", "application/vnd.sus-calendar");
            put("svd", "application/vnd.svd");
            put("sis", "application/vnd.symbian.install");
            put("xsm", "application/vnd.syncml+xml");
            put("bdm", "application/vnd.syncml.dm+wbxml");
            put("xdm", "application/vnd.syncml.dm+xml");
            put("tao", "application/vnd.tao.intent-module-archive");
            put("pcap", "application/vnd.tcpdump.pcap");
            put("tmo", "application/vnd.tmobile-livetv");
            put("tpt", "application/vnd.trid.tpt");
            put("mxs", "application/vnd.triscape.mxs");
            put("tra", "application/vnd.trueapp");
            put("ufd", "application/vnd.ufdl");
            put("utz", "application/vnd.uiq.theme");
            put("umj", "application/vnd.umajin");
            put("unityweb", "application/vnd.unity");
            put("uoml", "application/vnd.uoml+xml");
            put("vcx", "application/vnd.vcx");
            put("vsd", "application/vnd.visio");
            put("vis", "application/vnd.visionary");
            put("vsf", "application/vnd.vsf");
            put("wbxml", "application/vnd.wap.wbxml");
            put("wmlc", "application/vnd.wap.wmlc");
            put("wmlsc", "application/vnd.wap.wmlscriptc");
            put("wtb", "application/vnd.webturbo");
            put("nbp", "application/vnd.wolfram.player");
            put("wpd", "application/vnd.wordperfect");
            put("wqd", "application/vnd.wqd");
            put("stf", "application/vnd.wt.stf");
            put("xar", "application/vnd.xara");
            put("xfdl", "application/vnd.xfdl");
            put("hvd", "application/vnd.yamaha.hv-dic");
            put("hvs", "application/vnd.yamaha.hv-script");
            put("hvp", "application/vnd.yamaha.hv-voice");
            put("osf", "application/vnd.yamaha.openscoreformat");
            put("osfpvg", "application/vnd.yamaha.openscoreformat.osfpvg+xml");
            put("saf", "application/vnd.yamaha.smaf-audio");
            put("spf", "application/vnd.yamaha.smaf-phrase");
            put("cmp", "application/vnd.yellowriver-custom-menu");
            put("zir", "application/vnd.zul");
            put("zaz", "application/vnd.zzazz.deck+xml");
            put("vxml", "application/voicexml+xml");
            put("wgt", "application/widget");
            put("hlp", "application/winhlp");
            put("wsdl", "application/wsdl+xml");
            put("wspolicy", "application/wspolicy+xml");
            put("7z", "application/x-7z-compressed");
            put("abw", "application/x-abiword");
            put("ace", "application/x-ace-compressed");
            put("dmg", "application/x-apple-diskimage");
            put("aab", "application/x-authorware-bin");
            put("aam", "application/x-authorware-map");
            put("aas", "application/x-authorware-seg");
            put("bcpio", "application/x-bcpio");
            put("torrent", "application/x-bittorrent");
            put("blb", "application/x-blorb");
            put("bz", "application/x-bzip");
            put("bz2", "application/x-bzip2");
            put("cbr", "application/x-cbr");
            put("vcd", "application/x-cdlink");
            put("cfs", "application/x-cfs-compressed");
            put("chat", "application/x-chat");
            put("pgn", "application/x-chess-pgn");
            put("nsc", "application/x-conference");
            put("cpio", "application/x-cpio");
            put("csh", "application/x-csh");
            put("deb", "application/x-debian-package");
            put("dgc", "application/x-dgc-compressed");
            put("dir", "application/x-director");
            put("wad", "application/x-doom");
            put("ncx", "application/x-dtbncx+xml");
            put("dtb", "application/x-dtbook+xml");
            put("res", "application/x-dtbresource+xml");
            put("dvi", "application/x-dvi");
            put("evy", "application/x-envoy");
            put("eva", "application/x-eva");
            put("bdf", "application/x-font-bdf");
            put("gsf", "application/x-font-ghostscript");
            put("psf", "application/x-font-linux-psf");
            put("otf", "application/x-font-otf");
            put("pcf", "application/x-font-pcf");
            put("snf", "application/x-font-snf");
            put("ttf", "application/x-font-ttf");
            put("pfa", "application/x-font-type1");
            put("woff", "application/x-font-woff");
            put("arc", "application/x-freearc");
            put("spl", "application/x-futuresplash");
            put("gca", "application/x-gca-compressed");
            put("ulx", "application/x-glulx");
            put("gnumeric", "application/x-gnumeric");
            put("gramps", "application/x-gramps-xml");
            put("gtar", "application/x-gtar");
            put("hdf", "application/x-hdf");
            put("install", "application/x-install-instructions");
            put("iso", "application/x-iso9660-image");
            put("jnlp", "application/x-java-jnlp-file");
            put("latex", "application/x-latex");
            put("lzh", "application/x-lzh-compressed");
            put("mie", "application/x-mie");
            put("prc", "application/x-mobipocket-ebook");
            put("application", "application/x-ms-application");
            put("lnk", "application/x-ms-shortcut");
            put("wmd", "application/x-ms-wmd");
            put("wmz", "application/x-ms-wmz");
            put("xbap", "application/x-ms-xbap");
            put("mdb", "application/x-msaccess");
            put("obd", "application/x-msbinder");
            put("crd", "application/x-mscardfile");
            put("clp", "application/x-msclip");
            put("exe", "application/x-msdownload");
            put("mvb", "application/x-msmediaview");
            put("wmf", "application/x-msmetafile");
            put("mny", "application/x-msmoney");
            put("pub", "application/x-mspublisher");
            put("scd", "application/x-msschedule");
            put("trm", "application/x-msterminal");
            put("wri", "application/x-mswrite");
            put("nc", "application/x-netcdf");
            put("nzb", "application/x-nzb");
            put("p12", "application/x-pkcs12");
            put("p7b", "application/x-pkcs7-certificates");
            put("p7r", "application/x-pkcs7-certreqresp");
            put("rar", "application/x-rar-compressed");
            put("rar", "application/x-rar");
            put("ris", "application/x-research-info-systems");
            put("sh", "application/x-sh");
            put("shar", "application/x-shar");
            put("swf", "application/x-shockwave-flash");
            put("xap", "application/x-silverlight-app");
            put("sql", "application/x-sql");
            put("sit", "application/x-stuffit");
            put("sitx", "application/x-stuffitx");
            put("srt", "application/x-subrip");
            put("sv4cpio", "application/x-sv4cpio");
            put("sv4crc", "application/x-sv4crc");
            put("t3", "application/x-t3vm-image");
            put("gam", "application/x-tads");
            put("tar", "application/x-tar");
            put("tcl", "application/x-tcl");
            put("tex", "application/x-tex");
            put("tfm", "application/x-tex-tfm");
            put("texinfo", "application/x-texinfo");
            put("obj", "application/x-tgif");
            put("ustar", "application/x-ustar");
            put("src", "application/x-wais-source");
            put("der", "application/x-x509-ca-cert");
            put("fig", "application/x-xfig");
            put("xlf", "application/x-xliff+xml");
            put("xpi", "application/x-xpinstall");
            put("xz", "application/x-xz");
            put("z1", "application/x-zmachine");
            put("xaml", "application/xaml+xml");
            put("xdf", "application/xcap-diff+xml");
            put("xenc", "application/xenc+xml");
            put("xhtml", "application/xhtml+xml");
            put("xml", "application/xml");
            put("dtd", "application/xml-dtd");
            put("xop", "application/xop+xml");
            put("xpl", "application/xproc+xml");
            put("xslt", "application/xslt+xml");
            put("xspf", "application/xspf+xml");
            put("mxml", "application/xv+xml");
            put("yang", "application/yang");
            put("yin", "application/yin+xml");
            put("zip", "application/zip");
            put("adp", "audio/adpcm");
            put("au", "audio/basic");
            put("mid", "audio/midi");
            put("mp4a", "audio/mp4");
            put("mpga", "audio/mpeg");
            put("oga", "audio/ogg");
            put("s3m", "audio/s3m");
            put("sil", "audio/silk");
            put("uva", "audio/vnd.dece.audio");
            put("eol", "audio/vnd.digital-winds");
            put("dra", "audio/vnd.dra");
            put("dts", "audio/vnd.dts");
            put("dtshd", "audio/vnd.dts.hd");
            put("lvp", "audio/vnd.lucent.voice");
            put("pya", "audio/vnd.ms-playready.media.pya");
            put("ecelp4800", "audio/vnd.nuera.ecelp4800");
            put("ecelp7470", "audio/vnd.nuera.ecelp7470");
            put("ecelp9600", "audio/vnd.nuera.ecelp9600");
            put("rip", "audio/vnd.rip");
            put("weba", "audio/webm");
            put("aac", "audio/x-aac");
            put("aif", "audio/x-aiff");
            put("caf", "audio/x-caf");
            put("flac", "audio/x-flac");
            put("mka", "audio/x-matroska");
            put("m3u", "audio/x-mpegurl");
            put("wax", "audio/x-ms-wax");
            put("wma", "audio/x-ms-wma");
            put("ram", "audio/x-pn-realaudio");
            put("rmp", "audio/x-pn-realaudio-plugin");
            put("wav", "audio/x-wav");
            put("xm", "audio/xm");
            put("cdx", "chemical/x-cdx");
            put("cif", "chemical/x-cif");
            put("cmdf", "chemical/x-cmdf");
            put("cml", "chemical/x-cml");
            put("csml", "chemical/x-csml");
            put("xyz", "chemical/x-xyz");
            put("bmp", "image/bmp");
            put("cgm", "image/cgm");
            put("g3", "image/g3fax");
            put("gif", "image/gif");
            put("ief", "image/ief");
            put("jpeg", "image/jpeg");
            put("ktx", "image/ktx");
            put("png", "image/png");
            put("btif", "image/prs.btif");
            put("sgi", "image/sgi");
            put("svg", "image/svg+xml");
            put("tiff", "image/tiff");
            put("psd", "image/vnd.adobe.photoshop");
            put("uvi", "image/vnd.dece.graphic");
            put("sub", "image/vnd.dvb.subtitle");
            put("djvu", "image/vnd.djvu");
            put("dwg", "image/vnd.dwg");
            put("dxf", "image/vnd.dxf");
            put("fbs", "image/vnd.fastbidsheet");
            put("fpx", "image/vnd.fpx");
            put("fst", "image/vnd.fst");
            put("mmr", "image/vnd.fujixerox.edmics-mmr");
            put("rlc", "image/vnd.fujixerox.edmics-rlc");
            put("mdi", "image/vnd.ms-modi");
            put("wdp", "image/vnd.ms-photo");
            put("npx", "image/vnd.net-fpx");
            put("wbmp", "image/vnd.wap.wbmp");
            put("xif", "image/vnd.xiff");
            put("webp", "image/webp");
            put("3ds", "image/x-3ds");
            put("ras", "image/x-cmu-raster");
            put("cmx", "image/x-cmx");
            put("fh", "image/x-freehand");
            put("ico", "image/x-icon");
            put("sid", "image/x-mrsid-image");
            put("pcx", "image/x-pcx");
            put("pic", "image/x-pict");
            put("pnm", "image/x-portable-anymap");
            put("pbm", "image/x-portable-bitmap");
            put("pgm", "image/x-portable-graymap");
            put("ppm", "image/x-portable-pixmap");
            put("rgb", "image/x-rgb");
            put("tga", "image/x-tga");
            put("xbm", "image/x-xbitmap");
            put("xpm", "image/x-xpixmap");
            put("xwd", "image/x-xwindowdump");
            put("eml", "message/rfc822");
            put("igs", "model/iges");
            put("msh", "model/mesh");
            put("dae", "model/vnd.collada+xml");
            put("dwf", "model/vnd.dwf");
            put("gdl", "model/vnd.gdl");
            put("gtw", "model/vnd.gtw");
            put("mts", "model/vnd.mts");
            put("vtu", "model/vnd.vtu");
            put("wrl", "model/vrml");
            put("x3db", "model/x3d+binary");
            put("x3dv", "model/x3d+vrml");
            put("x3d", "model/x3d+xml");
            put("appcache", "text/cache-manifest");
            put("ics", "text/calendar");
            put("css", "text/css");
            put("csv", "text/csv");
            put("html", "text/html");
            put("n3", "text/n3");
            put("txt", "text/plain");
            put("dsc", "text/prs.lines.tag");
            put("rtx", "text/richtext");
            put("sgml", "text/sgml");
            put("tsv", "text/tab-separated-values");
            put("t", "text/troff");
            put("ttl", "text/turtle");
            put("uri", "text/uri-list");
            put("vcard", "text/vcard");
            put("curl", "text/vnd.curl");
            put("dcurl", "text/vnd.curl.dcurl");
            put("scurl", "text/vnd.curl.scurl");
            put("mcurl", "text/vnd.curl.mcurl");
            put("sub", "text/vnd.dvb.subtitle");
            put("fly", "text/vnd.fly");
            put("flx", "text/vnd.fmi.flexstor");
            put("gv", "text/vnd.graphviz");
            put("3dml", "text/vnd.in3d.3dml");
            put("spot", "text/vnd.in3d.spot");
            put("jad", "text/vnd.sun.j2me.app-descriptor");
            put("wml", "text/vnd.wap.wml");
            put("wmls", "text/vnd.wap.wmlscript");
            put("s", "text/x-asm");
            put("c", "text/x-c");
            put("f", "text/x-fortran");
            put("p", "text/x-pascal");
            put("java", "text/x-java-source");
            put("opml", "text/x-opml");
            put("nfo", "text/x-nfo");
            put("etx", "text/x-setext");
            put("sfv", "text/x-sfv");
            put("uu", "text/x-uuencode");
            put("vcs", "text/x-vcalendar");
            put("vcf", "text/x-vcard");
            put("3gp", "video/3gpp");
            put("3g2", "video/3gpp2");
            put("h261", "video/h261");
            put("h263", "video/h263");
            put("h264", "video/h264");
            put("jpgv", "video/jpeg");
            put("jpm", "video/jpm");
            put("mj2", "video/mj2");
            put("mp4", "video/mp4");
            put("mpeg", "video/mpeg");
            put("ogv", "video/ogg");
            put("qt", "video/quicktime");
            put("uvh", "video/vnd.dece.hd");
            put("uvm", "video/vnd.dece.mobile");
            put("uvp", "video/vnd.dece.pd");
            put("uvs", "video/vnd.dece.sd");
            put("uvv", "video/vnd.dece.video");
            put("dvb", "video/vnd.dvb.file");
            put("fvt", "video/vnd.fvt");
            put("mxu", "video/vnd.mpegurl");
            put("pyv", "video/vnd.ms-playready.media.pyv");
            put("uvu", "video/vnd.uvvu.mp4");
            put("viv", "video/vnd.vivo");
            put("webm", "video/webm");
            put("f4v", "video/x-f4v");
            put("fli", "video/x-fli");
            put("flv", "video/x-flv");
            put("m4v", "video/x-m4v");
            put("mkv", "video/x-matroska");
            put("mng", "video/x-mng");
            put("asf", "video/x-ms-asf");
            put("vob", "video/x-ms-vob");
            put("wm", "video/x-ms-wm");
            put("wmv", "video/x-ms-wmv");
            put("wmx", "video/x-ms-wmx");
            put("wvx", "video/x-ms-wvx");
            put("avi", "video/x-msvideo");
            put("movie", "video/x-sgi-movie");
            put("smv", "video/x-smv");
            put("ice", "x-conference/x-cooltalk");
            put("mpg", "video/mpeg");
            put("mp3", "audio/mpeg");
            put("gz", "application/x-gzip");
            put("jpg", "image/jpeg");
            put("pps", "application/vnd.ms-powerpoint");
            put("mov", "video/quicktime");
        }};
    }

    public static void writeFileToResponse(FileDto file, HttpServletResponse response) {
        if (!cn.hutool.core.io.FileUtil.exist(file.getUri())) {
            log.error(
                    "获取文件文件不存在，文件路径：{}，UUID：{}",
                    file.getUri(),
                    file.getUuid()
            );
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "文件丢失"
            );
        }

        writeByteToResponse(
                cn.hutool.core.io.FileUtil.readBytes(file.getUri()),
                file.getName(),
                response
        );
    }

    public static void writeFileToResponse(File file, HttpServletResponse response) {
        if (!cn.hutool.core.io.FileUtil.exist(file.getUri())) {
            log.error(
                    "获取文件文件不存在，文件路径：{}，UUID：{}",
                    file.getUri(),
                    file.getUuid()
            );
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "文件丢失"
            );
        }

        writeByteToResponse(
                cn.hutool.core.io.FileUtil.readBytes(file.getUri()),
                file.getName(),
                response
        );
    }

    @SneakyThrows(IOException.class)
    public static void writeInputStreamToResponse(InputStream inputStream,
                                                  String fileName,
                                                  HttpServletResponse response) {
        writeByteToResponse(
                inputStream.readAllBytes(),
                fileName,
                response
        );
        IoUtil.close(inputStream);
    }

    @SneakyThrows(IOException.class)
    public static void writeByteToResponse(byte[] bytes, String fileName, HttpServletResponse response) {
        initResponse(bytes.length, fileName, response,true);
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        IoUtil.close(outputStream);
    }

    public static void initResponse(long byteLength, String fileName, HttpServletResponse response, boolean cache) {
        response.reset();
        response.setContentType(ContentType.OCTET_STREAM.getValue());
        response.setCharacterEncoding(CharsetUtil.UTF_8);

        //缓存7天
        if (cache){
            response.setHeader(
                    Header.CACHE_CONTROL.getValue(),
                    "max-age=" + 60 * 60 * 24 * 7
            );
        }
        response.setHeader(
                Header.CONTENT_DISPOSITION.getValue(),
                "attachment;filename=" + URLEncoder.encode(
                        fileName,
                        StandardCharsets.UTF_8
                )
        );
        response.setHeader(
                Header.CONTENT_LENGTH.getValue(),
                String.valueOf(byteLength)
        );
        response.setHeader(
                "Accept-Ranges",
                "bytes"
        );
    }

    public static String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getFileType(String fileName) {
        return FILE_EXT_FILE_TYPE_MAP.get(getFileExt(fileName));
    }

    @SneakyThrows({FileNotFoundException.class, IOException.class})
    public static void splitFileAndThen(java.io.File file, int count, Consumer<java.io.File> exec) {
        if (count == 1) {
            exec.accept(file);
            return;
        }

        //预分配文件所占用的磁盘空间，在磁盘创建一个指定大小的文件，“r”表示只读，“rw”支持随机读写
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        //计算文件大小
        long length = raf.length();
        System.out.println(length);
        //计算文件切片后每一份文件的大小
        long maxSize = length / count;

        System.out.println(maxSize);

        //定义初始文件的偏移量(读取进度)
        long offset = 0L;
        //开始切割文件
        for (int i = 0; i < count - 1; i++) {
            //标记初始化
            long fbegin = offset;
            //分割第几份文件
            long fend = (i + 1) * maxSize;
            //写入文件
            WriteResult res = getWrite(file, i, fbegin, fend);
            offset = res.endPointer;

            exec.accept(res.file);

            cn.hutool.core.io.FileUtil.del(res.file);
        }

        //剩余部分文件写入到最后一份(如果不能平平均分配的时候)
        if (length - offset > 0) {
            //写入文件
            WriteResult res = getWrite(file, count - 1, offset, length);

            exec.accept(res.file);
            cn.hutool.core.io.FileUtil.del(res.file);
        }
    }

    /**
     * 指定文件每一份的边界，写入不同文件中
     *
     * @param file  源文件
     * @param index 源文件的顺序标识
     * @param begin 开始指针的位置
     * @param end   结束指针的位置
     * @return long
     */
    @SneakyThrows({FileNotFoundException.class, IOException.class})
    public static WriteResult getWrite(java.io.File file, int index, long begin, long end) {

        long endPointer = 0L;
        //申明文件切割后的文件磁盘
        RandomAccessFile in = new RandomAccessFile(file, "r");
        //定义一个可读，可写的文件并且后缀名为.tmp的二进制文件
        java.io.File tmp = new java.io.File(file + "_" + index + ".tmp");
        RandomAccessFile out = new RandomAccessFile(tmp, "rw");

        //申明具体每一文件的字节数组
        byte[] b = new byte[1024];
        int n;
        //从指定位置读取文件字节流
        in.seek(begin);
        //判断文件流读取的边界
        while (in.getFilePointer() <= end && (n = in.read(b)) != -1) {
            //从指定每一份文件的范围，写入不同的文件
            out.write(b, 0, n);
        }

        //定义当前读取文件的指针
        endPointer = in.getFilePointer();

        //关闭输入流
        in.close();
        //关闭输出流
        out.close();

        return new WriteResult(tmp, endPointer);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class WriteResult {
        private java.io.File file;
        private long endPointer;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class SplitFileResult {
        private java.io.File[] files;
        //删除分片文件的方法
        private Runnable delete;
    }

}
