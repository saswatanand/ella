import re
import os

methodsig_re = re.compile(r"^<(?P<ms_class>[^:]+): (?P<ms_ret>(?:\[)*(?:B|C|D|F|I|J|S|V|Z|L[\w/\$]+;)) (?P<ms_name>'[\w<>\$]+'|[\w<>\$]+)\((?P<ms_args>(?:(?:\[)*(?:B|C|D|F|I|J|S|V|Z|L[\w/\$]+;))*)\)>$")

class CoverageSet:
	
	def __init__(self):
		self.total_elements = 0
		self.covered_elements = 0
		self.elements_coverage = {}
	
	def _register_element(self, elem):
		if elem not in self.elements_coverage:
			self.elements_coverage[elem] = False
			self.total_elements += 1
	
	def _set_covered_element(self, elem):
		assert elem in self.elements_coverage
		if not self.elements_coverage[elem]:
			self.elements_coverage[elem] = True
			self.covered_elements += 1
	
	def announce_method(self, method_sig):
		pass
	
	def notify_covered_method(self, method_sig):
		pass
	
	def get_current_coverage(self):
		return str(100*(1.0*self.covered_elements/self.total_elements)) + "%" + " ( " + str(self.covered_elements) + " / " + str(self.total_elements) + " )"

class MethodCoverageSet(CoverageSet):
	
	def __init__(self):
		CoverageSet.__init__(self)
	
	def announce_method(self, method_sig):
		self._register_element(method_sig)
	
	def notify_covered_method(self, method_sig):
		self._set_covered_element(method_sig)
			
class ActivityCoverageSet(CoverageSet):

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
		CoverageSet.__init__(self)
		self.manifest = manifest
		self._activity_names = [a.name for a in self.manifest.activities]
		
	def announce_method(self, method_sig):
		activity = self._to_activity(method_sig)
		if activity != None:
			self._register_element(activity)
	
	def notify_covered_method(self, method_sig):
		activity = self._to_activity(method_sig)
		if activity != None:
			self._set_covered_element(activity)

class CoverageGraph(CoverageSet):
	
	def __init__(self):
		CoverageSet.__init__(self)
		self.X = []
		self.Y = []
		self.starttime = -1
		
	def setStartTime(self, starttime):
		self.starttime = starttime
	
	def _set_covered_element(self, elem, time):
		CoverageSet._set_covered_element(self, elem)
		self.X.append((time - self.starttime)/1000.0)
		self.Y.append(100*(1.0*self.covered_elements/self.total_elements))
	
	def announce_method(self, method_sig):
		pass
	
	def notify_covered_method(self, method_sig, time):
		pass
		
	def output_plot(self, out_dir, filename, Xlabel, Ylabel):
		from matplotlib import pyplot
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
