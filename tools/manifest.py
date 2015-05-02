from lxml import etree

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
