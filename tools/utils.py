import hashlib
import os

_ella_settings = None

def _get_settings_path():
	return os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)),"..","ella.settings"))

def get_ella_settings():
	global _ella_settings
	if _ella_settings != None:
		return _ella_settings
	settings_file = _get_settings_path()
	if not os.path.exists(settings_file):
		print "Please copy wrench.settings.template into wrench.settings and " + \
			  "set the configuration parameters in that file."
		exit(1)
	else:
		_ella_settings = {}
		with open(settings_file,'r') as f:
			for line in f:
				line = line.strip()
				if line == "" or line.startswith("#"): continue
				parts = line.split("=")
				assert len(parts) == 2
				_ella_settings[parts[0]] = parts[1]
	return _ella_settings

def apk_name_to_ellaout_dir(apk_name):
	appId = apk_name.replace('/','_');
	if len(appId) > 100:
		appId = hashlib.sha256(appId).hexdigest();
	return os.path.join(os.path.expanduser(get_ella_settings()["ella.outdir"]),appId)

class MalformedCoverageDatFileException(Exception):
	pass

class UnknownCoverageDatFormatVersion(Exception):
	pass
	
def parse_covdat_headers(covdat_file):
	headers = {}
	with open(covdat_file) as f:
		for line in f:
			line = line.strip()
			if line == "###": break # End of headers
			if line == "" or line.startswith("#"): continue
			parts = line.split(":")
			if(len(parts) != 2):
				raise MalformedCoverageDatFileException("Unexpected line in headers: " + line)
			headers[parts[0]] = parts[1]
	if "version" not in headers:
		raise MalformedCoverageDatFileException("Missing mandatory version field in headers.")
	if headers["version"] != "1":
		raise UnknownCoverageDatFormatVersion("coverage.dat declares version " + headers["version"] + " (only version 1 is currently supported)")
	if "recorder" not in headers:
		raise MalformedCoverageDatFileException("Missing mandatory recorder field in headers.")
	return headers
