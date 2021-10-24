package bletch.tektopiathief.core;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ModDetails {
	private static final int VersionMajor = 1;
	private static final int VersionMinor = 0;
	private static final int VersionRevision = 0;

	public static final String MOD_ID = "tektopiathief";
	public static final String MOD_NAME = "TektopiaThief";

	public static final String MOD_VERSION = VersionMajor + "." + VersionMinor + "." + VersionRevision;
	public static final String MOD_DEPENDENCIES = "required-after:minecraft@[1.12.2];required-after:forge@[14.23.5.2768,);required-after:tektopia@[1.0.0,);before:tektopiainformation";

	public static final String MOD_UPDATE_URL="https://raw.githubusercontent.com/Bletch1971/" + MOD_NAME + "/master/1.0/updateforge.json";
			
	public static final String MOD_SERVER_PROXY_CLASS = "bletch." + MOD_ID + ".core.ModCommonProxy";
	public static final String MOD_CLIENT_PROXY_CLASS = "bletch." + MOD_ID + ".core.ModClientProxy";

	public static final String MOD_ID_MINECRAFT = "minecraft";
	public static final String MOD_ID_FORGE = "forge";
	public static final String MOD_ID_TEKTOPIA = "tektopia";
	
	public static final String PATH_DEBUG = "/debug";
	public static final String FILE_DEBUGLOG = PATH_DEBUG + "/" + MOD_ID + ".txt";
}
