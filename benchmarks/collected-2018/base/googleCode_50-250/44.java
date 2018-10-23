// https://searchcode.com/api/result/8190097/

package info.bliki.wiki.client.filter;

import java.util.ArrayList;
import java.util.HashMap;


public class WPList {

  ArrayList fListElements;

  public WPList() {
    fListElements = new ArrayList();
  }

  /**
   * @param o
   * @return
   */
  public boolean add(Object o) {
    return fListElements.add(o);
  }

  /**
   * @param index
   * @return
   */
  public Object get(int index) {
    return fListElements.get(index);
  }

  /**
   * @return
   */
  public int size() {
    return fListElements.size();
  }
/**
 * render only the list tags not the content 
 * 
 * @param buf
 * @param src
 */
  public void render(StringBuffer buf, char[] src) {
    if (fListElements.size() > 0) {
//      int level;
//      int type;
      char[] currSeq;
      char[] lastSeq;
      WPListElement listElement;
      listElement = (WPListElement) fListElements.get(0);
//      level = listElement.getLevel();
//      type = listElement.getType();
      currSeq = listElement.getSequence();
      for (int i = 0; i < currSeq.length; i++) {
        if (currSeq[i] == '*') {
          buf.append("<ul>");
        } else {
          buf.append("<ol>");
        }
      }
      listElement.render(buf, src);

      for (int i = 1; i < fListElements.size(); i++) {
        lastSeq = currSeq;
        listElement = (WPListElement) fListElements.get(i);
        currSeq = listElement.getSequence();

        int startIndex = 0;
        if (lastSeq.length >= currSeq.length) {
          for (int l = 0; l < currSeq.length; l++) {
            if (currSeq[l] != lastSeq[l]) {
              break;
            }
            startIndex++;
          }
        } else {
          for (int l = 0; l < lastSeq.length; l++) {
            if (currSeq[l] != lastSeq[l]) {
              break;
            }
            startIndex++;
          }
        }
        if (lastSeq.length >= startIndex) {
          // reduce last list level:
          for (int j = lastSeq.length - 1; j >= startIndex; j--) {
            if (lastSeq[j] == '*') {
              buf.append("</ul>");
            } else {
              buf.append("</ol>");
            }
          }
        }
        if (i < fListElements.size() - 1) {
          if (currSeq.length >= startIndex) {
            // add next list level
            for (int j = startIndex; j < currSeq.length; j++) {
              if (currSeq[j] == '*') {
                buf.append("<ul>");
              } else {
                buf.append("<ol>");
              }
            }
          }
        }

        ((WPListElement) listElement).render(buf, src);
      }

      // reduce from currSeq
      for (int i = 0; i < currSeq.length; i++) {
        if (currSeq[i] == '*') {
          buf.append("</ul>");
        } else {
          buf.append("</ol>");
        }
      }
    }
  }

  /**
   * render the list tags and filter the content 
   * 
   * @param buf
   * @param src
   * @param recursionLevel
   */
  public void filter(StringBuffer buf, String src, HashMap wikiSettings, int recursionLevel) {
    if (fListElements.size() > 0) {
//      int level;
//      int type;
      char[] currSeq;
      char[] lastSeq;
      WPListElement listElement;
      listElement = (WPListElement) fListElements.get(0);
//      level = listElement.getLevel();
//      type = listElement.getType();
      currSeq = listElement.getSequence();
      for (int i = 0; i < currSeq.length; i++) {
        if (currSeq[i] == '*') {
          buf.append("<ul>");
        } else {
          buf.append("<ol>");
        }
      }
      listElement.filter(buf, src, wikiSettings, recursionLevel);

      for (int i = 1; i < fListElements.size(); i++) {
        lastSeq = currSeq;
        listElement = (WPListElement) fListElements.get(i);
        currSeq = listElement.getSequence();

        int startIndex = 0;
        if (lastSeq.length >= currSeq.length) {
          for (int l = 0; l < currSeq.length; l++) {
            if (currSeq[l] != lastSeq[l]) {
              break;
            }
            startIndex++;
          }
        } else {
          for (int l = 0; l < lastSeq.length; l++) {
            if (currSeq[l] != lastSeq[l]) {
              break;
            }
            startIndex++;
          }
        }
        if (lastSeq.length >= startIndex) {
          // reduce last list level:
          for (int j = lastSeq.length - 1; j >= startIndex; j--) {
            if (lastSeq[j] == '*') {
              buf.append("</ul>");
            } else {
              buf.append("</ol>");
            }
          }
        }
        if (i < fListElements.size()) {
          if (currSeq.length >= startIndex) {
            // add next list level
            for (int j = startIndex; j < currSeq.length; j++) {
              if (currSeq[j] == '*') {
                buf.append("<ul>");
              } else {
                buf.append("<ol>");
              }
            }
          }
        }

        ((WPListElement) listElement).filter(buf, src, wikiSettings, recursionLevel);
      }

      // reduce from currSeq
      for (int i = 0; i < currSeq.length; i++) {
        if (currSeq[i] == '*') {
          buf.append("</ul>");
        } else {
          buf.append("</ol>");
        }
      }
    }
  }
}
