package de.jost_net.JVerein.rest;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.rmi.Beitragsgruppe;
import de.jost_net.JVerein.rest.util.JsonUtil;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;

/**
 * REST-Bean zum Zugriff auf die Beitragsgruppen.
 */
@Doc("Jverein: Liefert Informationen über die Beitragsgruppen")
public class BeitragsgruppeBean implements AutoRestBean
{

  /*
  **
  * Liefert die Beitragsgruppen
  * 
  * @return Liste der Beitragsgruppen im JSON-Format.
  * @throws Exception
  */
 @Doc(value = "Liefert eine Liste der Beitragsgruppen im JSON-Format", example = "jverein/beitragsgruppe/list")
 @Path("/jverein/beitragsgruppe/list$")
 public Object getBeitragsgruppen() throws Exception
 {
   return JsonUtil
       .toJson(Einstellungen.getDBService().createList(Beitragsgruppe.class));
 }
 
 /**
  * Liefert ein Beitragsgruppe
  * 
  * @param id ID der Beitragsgruppe.
  * @return Beitragsgruppe im JSON-Format.
  * @throws Exception
  */
 @Doc(value = "Liefert eine Beitragsgruppe im JSON-Format", example = "jverein/beitragsgruppe/1")
 @Path("/jverein/beitragsgruppe/([0-9]{1,8})$")
 public Object getBeitragsgruppe(String id) throws Exception
 {
   Beitragsgruppe b = Einstellungen.getDBService().createObject(Beitragsgruppe.class, id);
   return JsonUtil.toJson(b);
 }
}
