package alessandro;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.io.Files;

public class JDeveloperWorkspace {
	
	@Parameter(names = "-workspace", required = true, description = "Workspace name.")
	private String workspace;
	
	@Parameter(names = "-path", required = true, description = "Directory start.")
	private String directory;
	
	@Parameter(names = "-name", required = true, description = "Batch filename.")
	private String name = "build.bat";

	protected Collection<File> listPomFiles(File directory) {
		Collection<File> pom_files = new ArrayList<File>();

		for (File file : directory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory()
						|| pathname.getName().endsWith("pom.xml");
			}
		})) {
			if (file.isDirectory()) {
				pom_files.addAll(listPomFiles(file));
			} else {
				pom_files.add(file);
			}
		}
		return pom_files;
	}

	protected Collection<Pom> parseFiles(Collection<File> files)
			throws ParserConfigurationException, SAXException, IOException {

		Collection<Pom> poms = new ArrayList<Pom>();

		for (File pom : files) {
			poms.add(parse(pom));
		}
		return Collections.unmodifiableCollection(poms);
	}

	private Pom parse(File xml) throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		doc.getDocumentElement().normalize();
		
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		Pom pom = new Pom();
		
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);			
			
			if (node.getNodeName().equals("groupId")){
				pom.setGroupId(node.getFirstChild().getNodeValue());
			} else if (node.getNodeName().equals("artifactId")){
				pom.setArtifactId(node.getFirstChild().getNodeValue());
			} else if (node.getNodeName().equals("version")){
				pom.setVersion(node.getFirstChild().getNodeValue());
			} else if (node.getNodeName().equals("profiles")) {
				NodeList profiles = node.getChildNodes();
				
				for (int j = 0; j < profiles.getLength(); j++){
					Node profile = profiles.item(j);
					NodeList profileItems = profile.getChildNodes();
					
					for(int k = 0; k < profileItems.getLength(); k++){
						Node profileNode = profileItems.item(k);
						
						if ("properties".equals(profileNode.getNodeName())){
							NodeList profileProperties = profileNode.getChildNodes();
							
							for (int l = 0; l < profileProperties.getLength(); l++){
								Node nodeProperty = profileProperties.item(l);
								if (nodeProperty.getNodeType() == Node.ELEMENT_NODE){
									pom.addProfileProperties(nodeProperty.getNodeName().trim(), 
											nodeProperty.getFirstChild().getNodeValue().trim());
								}
							}
						}
					}
				}
			}
		}
		return pom;
	}

	public void generateBatchFile(Collection<Pom> poms, File from) throws IOException {
		StringBuilder sb = new StringBuilder("mvn %1 %2 %3 %4 %5 %6 ");
		
		Map<String, String> globalProperties = new HashMap<String, String>();

		for (Pom pom : poms) {
			globalProperties.put(String.format("%s.version", pom.getArtifactId().trim()), pom.getVersion().trim());
						
			for (String property : pom.getProfileProperties().keySet()) {
				if (globalProperties.get(property) == null || (globalProperties.get(property).compareTo(pom.getProfileProperties().get(property)) < 0)) 
				{
					globalProperties.put(property, pom.getProfileProperties().get(property));
				} 
			}
		}		
		
		for(String property : globalProperties.keySet()){
			sb.append(String.format(" -D%s=%s", property, globalProperties.get(property)));
		}
		
		sb.append(" -Dworkspace=").append(String.format("%s/%s.jws", this.directory, this.workspace));
		
		Files.write(sb.toString().getBytes(), 
				new File(from.getAbsolutePath() + File.separatorChar + name));
	}
	
	public void generateBatchFile(File from) throws IOException, ParserConfigurationException, SAXException {
		generateBatchFile(this.parseFiles(this.listPomFiles(from)), from);
	}
	
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		JDeveloperWorkspace workspace = new JDeveloperWorkspace();
		
		JCommander jc = new JCommander();
		jc.addObject(workspace);
		
		if (args.length < 2)
			jc.usage();
		else {
			jc.parse(args);
			workspace.generateBatchFile(new File(workspace.directory));
		}
	}
}