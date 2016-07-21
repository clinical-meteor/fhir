package org.hl7.fhir.igtools.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import org.hl7.fhir.dstu3.validation.ValidationMessage;
import org.hl7.fhir.dstu3.validation.ValidationMessage.Source;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.igtools.publisher.HTLMLInspector.LoadedFile;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlParser;

public class HTLMLInspector {

  public class LoadedFile {
    private long lastModified;
    private XhtmlNode xhtml;
    private int iteration;
    private Set<String> targets = new HashSet<String>();

    public LoadedFile(long lastModified, XhtmlNode xhtml, int iteration) {
      this.lastModified = lastModified;
      this.xhtml = xhtml;
      this.iteration = iteration;
    }

    public long getLastModified() {
      return lastModified;
    }

    public int getIteration() {
      return iteration;
    }

    public void setIteration(int iteration) {
      this.iteration = iteration;
    }

    public XhtmlNode getXhtml() {
      return xhtml;
    }

    public Set<String> getTargets() {
      return targets;
    }

  }

  private String rootFolder;
  private Map<String, LoadedFile> cache = new HashMap<String, LoadedFile>();
  private int iteration = 0;

  public HTLMLInspector(String rootFolder) {
    this.rootFolder = rootFolder;    
  }

  public List<ValidationMessage> check() {
    iteration ++;

    List<ValidationMessage> messages = new ArrayList<ValidationMessage>();

    // list new or updated files
    List<String> loadList = new ArrayList<>();
    listFiles(rootFolder, loadList);

    checkGoneFiles();

    // load files
    for (String s : loadList)
      loadFile(s, messages);


    // check links
    for (String s : cache.keySet()) {
      LoadedFile lf = cache.get(s);
      if (lf.getXhtml() != null)
        checkLinks(s, "", lf.getXhtml(), messages);
    }
    return messages;
  }


  private void checkGoneFiles() {
    List<String> td = new ArrayList<String>();
    for (String s : cache.keySet()) {
      LoadedFile lf = cache.get(s);
      if (lf.getIteration() != iteration)
        td.add(s);
    }
    for (String s : td)
      cache.remove(s);
  }

  private void listFiles(String folder, List<String> loadList) {
    for (File f : new File(folder).listFiles()) {
      if (f.isDirectory()) {
        listFiles(f.getAbsolutePath() ,loadList);
      } else {
        LoadedFile lf = cache.get(f.getAbsolutePath());
        if (lf == null || lf.getLastModified() != f.lastModified())
          loadList.add(f.getAbsolutePath());
        else
          lf.setIteration(iteration);
      }
    }
  }

  private void loadFile(String s, List<ValidationMessage> messages) {
    File f = new File(s);
    XhtmlNode x = null;
    try {
      x = new XhtmlParser().parse(new FileInputStream(f), null);
    } catch (FHIRFormatError | IOException e) {
      x = null;
      messages.add(new ValidationMessage(Source.Publisher, IssueType.STRUCTURE, s, e.getMessage(), IssueSeverity.ERROR));
    }
    LoadedFile lf = new LoadedFile(f.lastModified(), x, iteration);
    cache.put(s, lf);
    if (x != null) {
      checkHtmlStructure(s, x, messages);
      listTargets(x, lf.getTargets());
    }
  }

  private void checkHtmlStructure(String s, XhtmlNode x, List<ValidationMessage> messages) {
    if (x.getNodeType() == NodeType.Document)
      x = x.getChildNodes().get(0);
    if (!"html".equals(x.getName()))
      messages.add(new ValidationMessage(Source.Publisher, IssueType.STRUCTURE, s, "Root node must be 'html', but is "+x.getName(), IssueSeverity.ERROR));
    // todo: check secure?
  }

  private void listTargets(XhtmlNode x, Set<String> targets) {
    if ("a".equals(x.getName()) && x.hasAttribute("name"))
      targets.add(x.getAttribute("name"));
    for (XhtmlNode c : x.getChildNodes())
      listTargets(c, targets);
  }

  private void checkLinks(String s, String path, XhtmlNode x, List<ValidationMessage> messages) {
    path = path + "/"+ x.getName();
    if ("a".equals(x.getName()) && x.hasAttribute("href"))
      checkResolveLink(s, path, x.getAttribute("href"), messages);
    for (XhtmlNode c : x.getChildNodes())
      checkLinks(s, path, c, messages);
  }

  private void checkResolveLink(String s, String path, String ref, List<ValidationMessage> messages) {
    boolean resolved = false;
    if (!resolved)
      messages.add(new ValidationMessage(Source.Publisher, IssueType.NOTFOUND, s+"#"+path, "The link '"+ref+" cannot be resolved", IssueSeverity.ERROR));
  }


}
