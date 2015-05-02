import inspect
from lxml import etree
from matplotlib import pyplot
import re
import os
import sys

methodsig_re = re.compile(r"^<(?P<ms_class>[^:]+): (?P<ms_ret>(?:\[)*(?:B|C|D|F|I|J|S|V|Z|L[\w/\$]+;)) (?P<ms_name>'[\w<>\$]+'|[\w<>\$]+)\((?P<ms_args>(?:(?:\[)*(?:B|C|D|F|I|J|S|V|Z|L[\w/\$]+;))*)\)>$")

def read_coverage(covf):
	for line in covf:
		line = line.strip()
		parts = line.split(" ")
		assert len(parts) == 2
		time = parts[0]
		method_id = parts[1]
		#if time == "0": continue # FIXME: Figure out how it's possible for Ella to write 0 as the time. FIXED?
		yield (int(time), int(method_id))

class ManifestActivity: 
	
	def __init__(self,name):
		self.name = name

class Manifest:

	def __init__(self,manifest_path):
		self.activities = []
		with open(manifest_path) as f:
			tree = etree.parse(f)
			root = tree.getroot()
			assert root.tag == "manifest"
			self.package = root.attrib["package"]
			application_elems = []
			for element in root.iter("*"):
				if element.tag == "application":
					application_elems.append(element)
			assert len(application_elems) == 1
			app = application_elems[0]
			for element in app.iter("*"):
				if element.tag == "activity":
					activity_name = element.attrib["{http://schemas.android.com/apk/res/android}name"]
					assert activity_name != None
					if activity_name.startswith("."):
						activity_name = self.package + activity_name
					self.activities.append(ManifestActivity(activity_name))
				
	
class CoverageGraph:
	
	def __init__(self):
		self.X = []
		self.Y = []
		self.total_elements = 0
		self.covered_elements = 0
		self.elements_coverage = {}
		self.starttime = -1
		
	def setStartTime(self, starttime):
		self.starttime = starttime
	
	def _register_element(self, elem):
		if elem not in self.elements_coverage:
			self.elements_coverage[elem] = False
			self.total_elements += 1
	
	def _set_covered_element(self, elem, time):
		assert elem in self.elements_coverage
		if not self.elements_coverage[elem]:
			self.elements_coverage[elem] = True
			self.covered_elements += 1
		self.X.append((time - self.starttime)/1000.0)
		self.Y.append(100*(1.0*self.covered_elements/self.total_elements))
	
	def announce_method(self, method_sig):
		pass
	
	def notify_covered_method(self, method_sig, time):
		pass
	
	def get_current_coverage(self):
		return str(100*(1.0*self.covered_elements/self.total_elements)) + "%" + " ( " + str(self.covered_elements) + " / " + str(self.total_elements) + " )"
		
	def output_plot(self, out_dir, filename, Xlabel, Ylabel):
		pyplot.plot(self.X, self.Y)
		pyplot.xlabel(Xlabel)
		pyplot.ylabel(Ylabel)
		pyplot.savefig(os.path.join(out_dir,filename))
		pyplot.close()
		
	def log_covered(self, out_path, covered=True):
		with open(out_path, 'w') as f:
			for elem in self.elements_coverage:
				if self.elements_coverage[elem] == covered:
					f.write(elem + "\n")

class MethodCoverageGraph(CoverageGraph):
	
	def __init__(self):
		CoverageGraph.__init__(self)
	
	def announce_method(self, method_sig):
		self._register_element(method_sig)
	
	def notify_covered_method(self, method_sig, time):
		self._set_covered_element(method_sig, time)

class CallbackCoverageGraph(CoverageGraph):

	def _is_callback(self, method_sig):
		methodsig_match = methodsig_re.match(method_sig)
		if not methodsig_match:
			raise Exception("Doesn't match method signature RE: " + method_sig)
		name = methodsig_match.group("ms_name")
		# FIXME: Use a proper callback detection analysis.
		if name.startswith("on"):
			return True
		else:
			return False
	
	def __init__(self):
		CoverageGraph.__init__(self)
	
	def announce_method(self, method_sig):
		if self._is_callback(method_sig):
			self._register_element(method_sig)
	
	def notify_covered_method(self, method_sig, time):
		if self._is_callback(method_sig):
			self._set_covered_element(method_sig, time)
			
class ActivityCoverageGraph(CoverageGraph):

	def _to_activity(self, method_sig):
		methodsig_match = methodsig_re.match(method_sig)
		if not methodsig_match:
			raise Exception("Doesn't match method signature RE: " + method_sig)
		klass = methodsig_match.group("ms_class")
		if klass in self._activity_names:
			return klass
		else:
			return None
	
	def __init__(self, manifest):
		CoverageGraph.__init__(self)
		self.manifest = manifest
		self._activity_names = [a.name for a in self.manifest.activities]
		
	def announce_method(self, method_sig):
		activity = self._to_activity(method_sig)
		if activity != None:
			self._register_element(activity)
	
	def notify_covered_method(self, method_sig, time):
		activity = self._to_activity(method_sig)
		if activity != None:
			self._set_covered_element(activity, time)
			
class ExcludePackageCoverageGraphFilter:
	
	def _exclude(self, method_sig):
		methodsig_match = methodsig_re.match(method_sig)
		if not methodsig_match:
			raise Exception("Doesn't match method signature RE: " + method_sig)
		klass = methodsig_match.group("ms_class")
		for prefix in self.exclude_prefixes:
			if klass.startswith(prefix):
				return True
		return False
	
	def __init__(self, base, exclude_prefixes):
		self.base = base
		self.exclude_prefixes = exclude_prefixes
		
	def setStartTime(self, starttime):
		self.base.setStartTime(starttime)
	
	def announce_method(self, method_sig):
		if not self._exclude(method_sig):
			self.base.announce_method(method_sig)
	
	def notify_covered_method(self, method_sig, time):
		if not self._exclude(method_sig):
			self.base.notify_covered_method(method_sig, time)
	
	def get_current_coverage(self):
		return self.base.get_current_coverage()
		
	def output_plot(self, out_dir, filename, Xlabel, Ylabel):
		self.base.output_plot(out_dir, filename, Xlabel, Ylabel)		
		

def graph_coverage(coverage_dir,indentationPrefix=""):
	covids = []
	common_libraries = []
	
	wrench_directory = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
	with open(os.path.join(wrench_directory,"common_libs.dat")) as f:
		for line in f:
			line = line.strip().split()[0]
			common_libraries.append(line)
	
	manifest = Manifest(os.path.join(coverage_dir,"AndroidManifest.xml"))
	
	coverage_graphs = {'method_coverage_all' : MethodCoverageGraph(),
						'callback_coverage_all' : CallbackCoverageGraph(), 
						'activity_coverage_all' : ActivityCoverageGraph(manifest),
						'method_coverage_nolibs' : ExcludePackageCoverageGraphFilter(MethodCoverageGraph(),common_libraries),
						'callback_coverage_nolibs' : ExcludePackageCoverageGraphFilter(CallbackCoverageGraph(),common_libraries),
						'activity_coverage_nolibs' : ExcludePackageCoverageGraphFilter(ActivityCoverageGraph(manifest),common_libraries)}
	
	activity_coverage = {}
	total_activities = 0
	total_activities_covered = 0
	
	with open(os.path.join(coverage_dir,"covids"),"r") as covidsf:
		for line in covidsf:
			method_sig = line.strip()
			covids.append(method_sig)
			for label, cgraph in coverage_graphs.items():
				cgraph.announce_method(method_sig)
	
	start_time = -1
	with open(os.path.join(coverage_dir,"coverage.dat"),"r") as coveragef:
		for (time, method_id) in read_coverage(coveragef):
			if start_time == -1:
				start_time = time
				for label, cgraph in coverage_graphs.items():
					cgraph.setStartTime(start_time)
			assert len(covids) > method_id
			method_sig = covids[method_id]
			for label, cgraph in coverage_graphs.items():
				cgraph.notify_covered_method(method_sig, time)
	
	# Plots
	coverage_graphs['method_coverage_all'].output_plot(coverage_dir, "method_coverage.png","Time (seconds)", "Method Coverage %")
	coverage_graphs['method_coverage_nolibs'].output_plot(coverage_dir, "method_coverage_nolibs.png","Time (seconds)", "Method Coverage %")
	coverage_graphs['callback_coverage_all'].output_plot(coverage_dir, "callback_coverage.png","Time (seconds)", "Callback Coverage %")
	coverage_graphs['callback_coverage_nolibs'].output_plot(coverage_dir, "callback_coverage_nolibs.png","Time (seconds)", "Callback Coverage %")
	coverage_graphs['activity_coverage_all'].output_plot(coverage_dir, "activity_coverage.png","Time (seconds)", "Activity Coverage %")
	coverage_graphs['activity_coverage_nolibs'].output_plot(coverage_dir, "activity_coverage_nolibs.png","Time (seconds)", "Activity Coverage %")
	
	coverage_graphs['activity_coverage_all'].log_covered(os.path.join(coverage_dir,"covered_activities.log"),True)
	coverage_graphs['activity_coverage_all'].log_covered(os.path.join(coverage_dir,"not_covered_activities.log"),False)
	
	print indentationPrefix + "Total method coverage:",coverage_graphs['method_coverage_all'].get_current_coverage()
	print indentationPrefix + "Total callback coverage:",coverage_graphs['callback_coverage_all'].get_current_coverage()
	print indentationPrefix + "Total activity coverage:",coverage_graphs['activity_coverage_all'].get_current_coverage()
	print indentationPrefix + "----"
	print indentationPrefix + "Total method coverage (excluding libraries):",coverage_graphs['method_coverage_nolibs'].get_current_coverage()
	print indentationPrefix + "Total callback coverage (excluding libraries):",coverage_graphs['callback_coverage_nolibs'].get_current_coverage()
	print indentationPrefix + "Total activity coverage (excluding libraries):",coverage_graphs['activity_coverage_nolibs'].get_current_coverage()

def main():
	assert len(sys.argv) == 2
	coverage_dir = sys.argv[1]
	graph_coverage(coverage_dir)

if __name__ == "__main__":
    main()
