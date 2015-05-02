import os
import sys

import coverage
import manifest
import utils

def get_latest_coverage_dat_file(ella_run_dir):
	coverage_dat_files = [ f for f in os.listdir(ella_run_dir) if os.path.isfile(os.path.join(ella_run_dir,f)) and f.startswith("coverage.dat.") ]
	latest = sorted(coverage_dat_files)[-1] # Assumes lexicographically sortable dates
	return os.path.join(ella_run_dir,latest)

def read_coverage(covf):
	for line in covf:
		line = line.strip()
		if ":" in line or line == "###": continue # Ignore header data
		parts = line.split(" ")
		assert len(parts) == 2
		time = parts[0]
		method_id = parts[1]
		#if time == "0": continue # FIXME: Figure out how it's possible for Ella to write 0 as the time. FIXED?
		yield (int(time), int(method_id))

def coverage_for_apk(apk_name, coverage_type):
	ella_run_dir = utils.apk_name_to_ellaout_dir(apk_name)
	cov_dat = get_latest_coverage_dat_file(ella_run_dir)
	headers = utils.parse_covdat_headers(cov_dat)
	if headers["recorder"] == "com.apposcopy.ella.runtime.MethodSequenceRecorder":
		if coverage_type == "method":
			cov = coverage.MethodCoverageGraph()
		elif coverage_type == "activity":
			am = manifest.Manifest(os.path.join(ella_run_dir,"apktool-out","AndroidManifest.xml"))
			cov = coverage.ActivityCoverageGraph(am)
		else:
			raise Exception("Not implemented: " + coverage_type + " coverage, for recorder " + headers["recorder"])
	elif headers["recorder"] == "com.apposcopy.ella.runtime.MethodCoverageRecorder":
		if coverage_type == "method":
			cov = coverage.MethodCoverageSet()
		elif coverage_type == "activity":
			am = manifest.Manifest(os.path.join(ella_run_dir,"apktool-out","AndroidManifest.xml"))
			cov = coverage.ActivityCoverageSet(am)
		else:
			raise Exception("Not implemented: " + coverage_type + " coverage, for recorder " + headers["recorder"])
	else:
		raise Exception("Unknown recorder type: " + headers["recorder"])
		
	covids = []
	with open(os.path.join(ella_run_dir,"covids"),"r") as covidsf:
		for line in covidsf:
			method_sig = line.strip()
			covids.append(method_sig)
			cov.announce_method(method_sig)
	
	if headers["recorder"] == "com.apposcopy.ella.runtime.MethodSequenceRecorder":
		start_time = -1
		with open(cov_dat,"r") as coveragef:
			for (time, method_id) in read_coverage(coveragef):
				if start_time == -1:
					start_time = time
					cov.setStartTime(start_time)
				assert len(covids) > method_id
				method_sig = covids[method_id]
				cov.notify_covered_method(method_sig, time)
	elif headers["recorder"] == "com.apposcopy.ella.runtime.MethodCoverageRecorder":
		with open(cov_dat,"r") as coveragef:
			for line in coveragef:
				line = line.strip()
				if ":" in line or line == "###": continue # Ignore header data
				method_id = int(line)
				assert len(covids) > method_id
				method_sig = covids[method_id]
				cov.notify_covered_method(method_sig)
	else:
		assert False # Unreachable
		
	if coverage_type == "method":
		print "Method coverage:",cov.get_current_coverage()
	elif coverage_type == "activity":
		print "Activity coverage:",cov.get_current_coverage()

def main():
	assert len(sys.argv) == 3
	apk_name = sys.argv[1]
	coverage_type = sys.argv[2]
	for ctype in coverage_type.split(","):
		coverage_for_apk(apk_name, ctype)

if __name__ == "__main__":
    main()
