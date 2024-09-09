package de.jost_net.JVerein.rest;

import de.jost_net.JVerein.rmi.Adresstyp;
import de.jost_net.JVerein.rmi.Beitragsgruppe;
import de.jost_net.JVerein.rmi.Buchungsart;
import de.jost_net.JVerein.rmi.Eigenschaft;
import de.jost_net.JVerein.rmi.Eigenschaften;
import de.jost_net.JVerein.rmi.Mitglied;
import de.jost_net.JVerein.rmi.SekundaereBeitragsgruppe;
import de.jost_net.JVerein.rmi.Zusatzbetrag;
import de.jost_net.JVerein.rmi.Zusatzfelder;
import de.jost_net.JVerein.util.Datum;
import de.jost_net.JVerein.util.EmailValidator;
import de.jost_net.OBanToo.SEPA.IBAN;
import de.jost_net.OBanToo.SEPA.SEPAException;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.datasource.rmi.ObjectNotFoundException;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;
import de.willuhn.util.ApplicationException;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.keys.ArtBeitragsart;
import de.jost_net.JVerein.keys.Beitragsmodel;
import de.jost_net.JVerein.keys.Datentyp;
import de.jost_net.JVerein.keys.IntervallZusatzzahlung;
import de.jost_net.JVerein.keys.Zahlungsrhythmus;
import de.jost_net.JVerein.keys.Zahlungstermin;
import de.jost_net.JVerein.keys.Zahlungsweg;
import de.jost_net.JVerein.rest.util.JsonUtil;

/**
 * REST-Bean zum Zugriff auf die Mitglieder.
 */
@Doc("Jverein: Liefert Informationen über die Mitglieder")
public class MitgliedBean implements AutoRestBean
{

  @Request
  private HttpServletRequest request = null;

  /**
   * Liefert die Mitglieder
   * 
   * @return Liste der Mitglieder im JSON-Format.
   * @throws Exception
   */
  @Doc(value = "Liefert eine Liste der Mitglieder im JSON-Format", example = "jverein/mitglied/list")
  @Path("/jverein/mitglied/list$")
  public Object getMitglieder() throws Exception
  {
    return JsonUtil
        .toJson(Einstellungen.getDBService().createList(Mitglied.class));
  }

  /**
   * Liefert ein Mitglieder
   * 
   * @param id
   *          ID des Mitglieds.
   * @return Mitglied im JSON-Format.
   * @throws Exception
   */
  @Doc(value = "Liefert eine Mitglied im JSON-Format", example = "jverein/mitglied/123")
  @Path("/jverein/mitglied/([0-9]{1,8})$")
  public Object getMitglieder(String id) throws Exception
  {
    Mitglied m = Einstellungen.getDBService().createObject(Mitglied.class, id);
    return JsonUtil.toJson(m);
  }

  /**
   * Erstellt ein neues Mitglied.
   * 
   * @return Die Eigenschaften des erstellten Mitglies.
   * @throws Exception
   */
  @Doc(value = "Erstellt ein neues Mitglied. "
      + "Die Funktion erwartet folgende Parameter via GET oder POST.<br/>"
      + "<ul>"
      + "  <li><b>personenart</b>: Personenart (n: natürliche Paerson; j: Juristische Person)</li>"
      + "  <li><b>adresstyp</b>: optional: Adresstyp (default 1 für mitglieder)</li>"
      + "  <li><b>anrede</b>: optional: Anrede</li>"
      + "  <li><b>titel</b>: optional: Titel</li>"
      + "  <li><b>name</b>: Name</li>" + "  <li><b>vorname</b>: Vorname</li>"
      + "  <li><b>adressierungszusatz</b>: optional: Adresszusatz</li>"
      + "  <li><b>strasse</b>: Strasse</li>" + "  <li><b>plz</b>: PLZ</li>"
      + "  <li><b>ort</b>: Ort</li>"
      + "  <li><b>staat</b>: optional: Staat</li>"
      + "  <li><b>geburtsdatum</b>: optional: Geburtsdatum</li>"
      + "  <li><b>geschlecht</b>: Geschlecht</li>"

      + "  <li><b>eintritt</b>: Eintrittsdatum</li>"
      + "  <li><b>beitragsgruppe</b>: Beitragsgruppe</li>"
      + "  <li><b>mandatdatum</b>: Mandatdatum</li>"
      + "  <li><b>mandatversion</b>: Mandatversion</li>"
      + "  <li><b>Optional: bic</b>: BIC</li>" + "  <li><b>iban</b>: IBAN</li>"

      + "  <li><b>externemitgliedsnummer</b>: optional: Externe Mitgliedsnummer</li>"
      + "  <li><b>zahlungsweg</b>: Optional: Zahlungsweg (1: Basislastschrift, 2: Überweisung, 3: Bar, 4: Durch Vollzahler)</li>"
      + "  <li><b>zahlungsrhytmus</b>: Optional: Zahlungsrthmus (12: jährlich, 6 halbjährlich, 3 vierteljährlich, default 1 monatlich)</li>"

      + "  <li><b>telefonprivat</b>: optional: Telefon</li>"
      + "  <li><b>telefondienstlich</b>: optional: Telefon dienstlich</li>"
      + "  <li><b>handy</b>: optional: handy</li>"
      + "  <li><b>email</b>: optional: Email</li>"
      + "  <li><b>individuellerbeitrag</b>: optional: Individueller Beitrag</li>"
      + "  <li><b>austritt</b>: optional: Austrittsdatum</li>"
      + "  <li><b>kuendigung</b>: optional: Kündigungsdatum</li>"
      + "  <li><b>sterbetag</b>: optional: Sterbedatum</li>"
      + "  <li><b>vermerk1</b>: optional: Vermerk1</li>"
      + "  <li><b>vermerk2</b>: optional: Vererk2</li>"

      + "  <li><b>ktoipersonenart</b>: optional: Kontoinhaber Personenart</li>"
      + "  <li><b>ktoianrede</b>: optional: Kontoinhaber Anrede</li>"
      + "  <li><b>ktoititel</b>: optional: Kontoinhaber Titel</li>"
      + "  <li><b>ktoiname</b>: optional: Kontoinhaber Name</li>"
      + "  <li><b>ktoivorname</b>: optional: Kontoinhaber Vorname</li>"
      + "  <li><b>ktoistrasse</b>: optional: Kontoinhaber Strasse</li>"
      + "  <li><b>ktoiadressierungszusatz</b>: optional: Kontoinhaber Adresszusatz</li>"
      + "  <li><b>ktoiplz</b>: optional: Kontoinhaber PLZ</li>"
      + "  <li><b>ktoiort</b>: optional: Kontoinhaber Ort</li>"
      + "  <li><b>ktoistaat</b>: optional: Kontoinhaber Staat</li>"
      + "  <li><b>ktoiemail</b>: optional: Kontoinhaber Email</li>"
      + "  <li><b>ktoigeschlecht</b>: optional: Kontoinhaber Geschlecht</li>"

      + "  <li><b>zahlungstermin</b>: Optional: Zahlungstermin (default 1: Monatlich\n"
      + "31: Vierteljährlich (Jan./Apr./Juli/Okt)\n"
      + "32: Vierteljährlich (Feb./Mai /Aug./Nov.)\n"
      + "33: Vierteljährlich (März/Juni/Sep./Dez.)\n"
      + "61: Halbjährlich (Jan./Juli)\n" + "62: Halbjährlich (Feb./Aug.)\n"
      + "63: Halbjährlich (März/Sep.)\n" + "64: Halbjährlich (Apr./Okt.)\n"
      + "65: Halbjährlich (Mai /Nov.)\n" + "66: Halbjährlich (Juni/Dez.)\n"
      + "1201: Jährlich (Jan.)\n" + "1202: Jährlich (Feb.)\n"
      + "1203: Jährlich (März)\n" + "1204: Jährlich (Apr.)\n"
      + "1205: Jährlich (Mai )\n" + "1206: Jährlich (Juni)\n"
      + "1207: Jährlich (Juli)\n" + "1208: Jährlich (Aug.)\n"
      + "1209: Jährlich (Sep.)\n" + "1210: Jährlich (Okt.)\n"
      + "1211: Jährlich (Nov.)\n" + "1212: Jährlich (Dez.))"
      + "</ul>", example = "jverein/mitglied/create")
  @Path("/jverein/mitglied/create$")
  public Object create() throws Exception
  {
    Mitglied m;
    try
    {
      m = (Mitglied) Einstellungen.getDBService().createObject(Mitglied.class,
          null);
      m = fill(m);
    }
    catch (NumberFormatException e)
    {
      JSONObject o = new JSONObject();
      o.put("error", "Kann nummer nicht parsen " + e.getMessage());
      return o;
    }
    catch (Exception e)
    {
      JSONObject o = new JSONObject();
      o.put("error", e.getMessage());
      return o;
    }
    return JsonUtil.toJson(m);
  }

  /**
   * Listet die Eigenschaften ein Mitglied.
   * 
   * @return Die Eigenschaften des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Liste der Eingenschaften eines Mitglied. ", example = "jverein/mitglied/123/eigenschaften")
  @Path("/jverein/mitglied/([0-9]{1,8})/eigenschaften$")
  public Object eigenschaftenList(String id) throws Exception
  {
    List<Eigenschaft> list = new ArrayList<Eigenschaft>();
    DBIterator<Eigenschaften> it = Einstellungen.getDBService()
        .createList(Eigenschaften.class);
    it.addFilter("mitglied = ?", id);
    while (it.hasNext())
    {
      Eigenschaften e = it.next();
      list.add(e.getEigenschaft());
    }

    return JsonUtil.toJson(list);
  }

  /**
   * Listet die Zusatzfelder eines Mitglied.
   * 
   * @return Die Zusatzfelder des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Liste der Zusatzfelder eines Mitglied. ", example = "jverein/mitglied/123/zusatzfelder")
  @Path("/jverein/mitglied/([0-9]{1,8})/zusatzfelder$")
  public Object zusatzfelderList(String id) throws Exception
  {
    Map<String, Object> o = new HashMap<String, Object>();
    DBIterator<Zusatzfelder> it = Einstellungen.getDBService()
        .createList(Zusatzfelder.class);
    it.addFilter("mitglied = ?", id);
    while (it.hasNext())
    {
      Zusatzfelder z = it.next();
      z.getFelddefinition().getLabel();
      switch (z.getFelddefinition().getDatentyp())
      {
        case Datentyp.DATUM:
          o.put(z.getFelddefinition().getName(), z.getFeldDatum());
          break;
        case Datentyp.GANZZAHL:
          o.put(z.getFelddefinition().getName(), z.getFeldGanzzahl());
          break;
        case Datentyp.JANEIN:
          o.put(z.getFelddefinition().getName(), z.getFeldJaNein());
          break;
        case Datentyp.WAEHRUNG:
          o.put(z.getFelddefinition().getName(), z.getFeldWaehrung());
          break;
        case Datentyp.ZEICHENFOLGE:
          o.put(z.getFelddefinition().getName(), z.getFeld());
          break;
      }
    }

    return o;
  }

  /**
   * Listet die sekundären Beitragsgruppen eines Mitglied.
   * 
   * @return Die sekundären Beitragsgruppen des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Liste der sekundären Beitragsgruppen eines Mitglied. ", example = "jverein/mitglied/123/sekundaer")
  @Path("/jverein/mitglied/([0-9]{1,8})/sekundaer$")
  public Object sekundaerList(String id) throws Exception
  {
    List<Beitragsgruppe> list = new ArrayList<Beitragsgruppe>();
    DBIterator<SekundaereBeitragsgruppe> it = Einstellungen.getDBService()
        .createList(SekundaereBeitragsgruppe.class);
    it.addFilter("mitglied = ?", id);
    while (it.hasNext())
    {
      SekundaereBeitragsgruppe bg = it.next();
      list.add(bg.getBeitragsgruppe());
    }

    return JsonUtil.toJson(list);
  }
  

  /**
   * Listet die zusatzbeitraege eines Mitglied.
   * 
   * @return Die zusatzbeiträge des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Liste der Zusatzbeiträge eines Mitglied. ", example = "jverein/mitglied/123/zusatzbeitrag")
  @Path("/jverein/mitglied/([0-9]{1,8})/zusatzbeitrag$")
  public Object zusatzbeitragList(String id) throws Exception
  {
    DBIterator<Zusatzbetrag> it = Einstellungen.getDBService()
        .createList(Zusatzbetrag.class);
    it.addFilter("mitglied = ?", id);

    return JsonUtil.toJson(it);
  }
  
  /**
   * Listet die offenen zusatzbeitraege eines Mitglied.
   * 
   * @return Die offenen zusatzbeiträge des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Liste der offenen Zusatzbeiträge eines Mitglied. ", example = "jverein/mitglied/123/zusatzbeitrag/open")
  @Path("/jverein/mitglied/([0-9]{1,8})/zusatzbeitrag/open$")
  public Object zusatzbeitragOpenList(String id) throws Exception
  {
    List<Zusatzbetrag> list = new ArrayList<Zusatzbetrag>();
    DBIterator<Zusatzbetrag> it = Einstellungen.getDBService()
        .createList(Zusatzbetrag.class);
    it.addFilter("mitglied = ?", id);
    
    while(it.hasNext())
    {
      Zusatzbetrag z = it.next();
      if(z.isAktiv(new Date()))
        list.add(z);
    }

    return JsonUtil.toJson(list);
  }

  /**
   * Zusatzbeitrag einem Mitglied zuweisen.
   * 
   * @return Der Zusatzbeitrrag.
   * @throws Exception
   */
  @Doc(value = "Einem Mitglied einen Zusatzbeitrag zuweisen.\n"
      + "Die Funktion erwartet folgende Parameter via GET oder POST.<br/>\n"
      + "<ul>\"\n" + "  <li><b>faelligkeit</b>: Fälligkeit</li>\n"
      + "  <li><b>buchungstext</b>: Buchungstext</li>\n"
      + "  <li><b>betrag</b>: Betrag</li>\n"
      + "  <li><b>startdatum</b>: Startdatum</li>\n"
      + "  <li><b>intervall</b>: optional: Intervall</li>\n"
      + "  <li><b>endedatum</b>: optional: endedatum</li>\n"
      + "  <li><b>buchungsart</b>: optional: Buchungsart</li>\n"
      + "  </ul>", example = "jverein/mitglied/123/zusatzbeitrag/add")
  @Path("/jverein/mitglied/([0-9]{1,8})/zusatzbeitrag/add$")
  public Object addZusatzbeitrag(String id) throws Exception
  {
    Zusatzbetrag z;
    try
    {
      Mitglied m = Einstellungen.getDBService().createObject(Mitglied.class,
          id);

      z = Einstellungen.getDBService()
          .createObject(Zusatzbetrag.class, null);

      z.setMitglied(Integer.parseInt(m.getID()));

      String buchungstext = request.getParameter("buchungstext");
      if (buchungstext != null && buchungstext.length() != 0)
      {
        z.setBuchungstext(buchungstext);
      }

      String betrag = request.getParameter("betrag");
      if (betrag != null && betrag.length() != 0)
      {
        z.setBetrag(Double.parseDouble(betrag));
      }
      else
        throw new ApplicationException(
            "Bitte Betrag angeben");

      String startdatum = request.getParameter("startdatum");
      if (startdatum != null && startdatum.length() != 0)
      {
        z.setStartdatum(Datum.toDate(startdatum));
      }
      

      String faelligkeit = request.getParameter("faelligkeit");
      if (faelligkeit != null && faelligkeit.length() != 0)
      {
        z.setFaelligkeit(Datum.toDate(faelligkeit));
      }
      else
        z.setFaelligkeit(z.getStartdatum());

      String intervall = request.getParameter("intervall");
      if (intervall != null && intervall.length() != 0)
      {
        if (IntervallZusatzzahlung.get(Integer.parseInt(intervall)) == null)
          throw new ApplicationException(
              "Intervall nicht vorhanden: " + intervall);
        z.setIntervall(Integer.parseInt(intervall));
      }
      else 
        z.setIntervall(IntervallZusatzzahlung.KEIN);

      String endedatum = request.getParameter("endedatum");
      if (endedatum != null && endedatum.length() != 0)
      {
        z.setEndedatum(Datum.toDate(endedatum));
      }

      String buchungsart = request.getParameter("buchungsart");
      if (buchungsart != null && buchungsart.length() != 0)
      {
        Buchungsart b = Einstellungen.getDBService()
            .createObject(Buchungsart.class, buchungsart);
        z.setBuchungsart(b);
      }
      z.store();
    }
    catch (NumberFormatException e)
    {
      JSONObject o = new JSONObject();
      o.put("error", "Kann nummer nicht parsen " + e.getMessage());
      return o;
    }
    catch (NullPointerException e)
    {
      JSONObject o = new JSONObject();
      o.put("error", "null Pointer Exception");
      return o;
    }
    catch (Exception e)
    {
      JSONObject o = new JSONObject();
      o.put("error", e.getMessage());
      return o;
    }

    return JsonUtil.toJson(z);
  }

  /**
   * Liefert die Eigenschaften eines Mitglied.
   * 
   * @return Die Eigenschaften des Mitglies.
   * @throws Exception
   */
  @Doc(value = "Ändert ein Mitglied. "
      + "mögliche Parameter siehe create (alle optional)", example = "jverein/mitglied/update/123")
  @Path("/jverein/mitglied/update/([0-9]{1,8})$")
  public Object update(String id) throws Exception
  {
    Mitglied m;
    try
    {
      m = (Mitglied) Einstellungen.getDBService().createObject(Mitglied.class,
          id);
      m = fill(m);
    }
    catch (NumberFormatException e)
    {
      JSONObject o = new JSONObject();
      o.put("error", "Kann nummer nicht parsen " + e.getMessage());
      return o;
    }
    catch (Exception e)
    {
      JSONObject o = new JSONObject();
      o.put("error", e.getMessage());
      return o;
    }
    return JsonUtil.toJson(m);
  }

  private Mitglied fill(Mitglied m) throws RemoteException,
      ApplicationException, ParseException, SEPAException
  {
    String adresstyp = request.getParameter("adresstyp");
    if (adresstyp != null && adresstyp.length() != 0)
    {
      try
      {
        Adresstyp at = (Adresstyp) Einstellungen.getDBService()
            .createObject(Adresstyp.class, adresstyp);
        m.setAdresstyp(Integer.valueOf(at.getID()));
      }
      catch (ObjectNotFoundException e)
      {
        throw new ApplicationException(
            "Adresstyp nicht vorhanden: " + adresstyp);
      }
    }
    else
    {
      if (m.getAdresstyp() == null)
        m.setAdresstyp(1);
    }

    String adressierungszusatz = request.getParameter("adressierungszusatz");
    if (adressierungszusatz != null && adressierungszusatz.length() != 0)
    {
      m.setAdressierungszusatz(adressierungszusatz);
    }

    String eintritt = request.getParameter("eintritt");
    if (eintritt != null && eintritt.length() != 0)
    {
      m.setEintritt(Datum.toDate(eintritt));
    }

    String austritt = request.getParameter("austritt");
    if (austritt != null && austritt.length() != 0)
    {
      try
      {
        if (m.getEintritt() == null
            || Datum.toDate(austritt).before(m.getEintritt()))
          throw new ApplicationException(
              "Austritt kann nicht vor Eintritt liegen");
        m.setAustritt(austritt);
      }
      catch (ParseException e)
      {
        throw new ApplicationException(
            "Ungültiges Datumsformat für austritt: " + austritt);
      }
    }

    String anrede = request.getParameter("anrede");
    if (anrede != null && anrede.length() != 0)
    {
      m.setAnrede(anrede);
    }

    String beitragsgruppe = request.getParameter("beitragsgruppe");
    if (adresstyp == null || adresstyp == "1")
    {
      if (beitragsgruppe != null && beitragsgruppe.length() != 0)
      {
        try
        {
          Beitragsgruppe bg = (Beitragsgruppe) Einstellungen.getDBService()
              .createObject(Beitragsgruppe.class, beitragsgruppe);
          if (bg.getSekundaer())
            throw new ApplicationException(
                "Beitragsgruppe ist sekundäre Beitragsgruppe: "
                    + beitragsgruppe);
          m.setBeitragsgruppe(Integer.valueOf(bg.getID()));
          if (bg.getBeitragsArt() != ArtBeitragsart.FAMILIE_ANGEHOERIGER)
          {
            m.setZahlerID(null);
          }
        }
        catch (ObjectNotFoundException e)
        {
          throw new ApplicationException(
              "Beitragsgruppe nicht gefunden: " + beitragsgruppe);
        }
      }
      else
      {
        if (m.getBeitragsgruppe() == null)
          throw new ApplicationException("Beitragsgruppe fehlt");
      }
    }

    if (Einstellungen.getEinstellung().getIndividuelleBeitraege())
    {
      String individuellerBeitrag = request
          .getParameter("individuellerbeitrag");
      if (individuellerBeitrag != null)
      {
        m.setIndividuellerBeitrag(Double.valueOf(individuellerBeitrag));
      }
      else
      {
        m.setIndividuellerBeitrag(null);
      }
    }

    String zahlungsweg = request.getParameter("zahlungsweg");
    if (zahlungsweg != null && zahlungsweg.length() != 0)
    {
      if (Zahlungsweg.get(Integer.parseInt(zahlungsweg)) == null)
        throw new ApplicationException("Zahlungsweg ungültig: " + zahlungsweg);
      if (Integer.parseInt(zahlungsweg) == 4//Zahlungsweg.VOLLZAHLER hardcodiert aus Komapilitätsgründen zu 2.8.22
          && m.getBeitragsgruppe()
              .getBeitragsArt() != ArtBeitragsart.FAMILIE_ANGEHOERIGER)
        throw new ApplicationException("Zahlungsweg VOLLZAHLER("
            + 4/*Zahlungsweg.VOLLZAHLER*/ + ") nur für Familienangehörige");
      m.setZahlungsweg(Integer.parseInt(zahlungsweg));
    }
    else
    {
      if (m.getZahlungsweg() == null)
        m.setZahlungsweg(Einstellungen.getEinstellung().getZahlungsweg());
    }

    if (Einstellungen.getEinstellung()
        .getBeitragsmodel() == Beitragsmodel.MONATLICH12631)
    {
      String zahlungsrhytmus = request.getParameter("zahlungsrhytmus");
      if (zahlungsrhytmus != null && zahlungsrhytmus.length() != 0)
      {
        if (Zahlungsrhythmus.get(Integer.parseInt(zahlungsrhytmus)) == null)
          throw new ApplicationException(
              "Ungültiger Zahlungsrythmus: " + zahlungsrhytmus);
        m.setZahlungsrhythmus(Integer.parseInt(zahlungsrhytmus));
      }
      else
      {
        if (m.getZahlungsrhythmus() == null)
          m.setZahlungsrhythmus(Zahlungsrhythmus.MONATLICH);
      }
    }
    else
      m.setZahlungsrhythmus(Zahlungsrhythmus.MONATLICH);

    if (Einstellungen.getEinstellung()
        .getBeitragsmodel() == Beitragsmodel.FLEXIBEL)
    {
      String zahlungstermin = request.getParameter("zahlungstermin");
      if (zahlungstermin != null && zahlungstermin.length() != 0)
      {
        if (Zahlungstermin.getByKey(Integer.parseInt(zahlungstermin)) == null)
          throw new ApplicationException(
              "Ungültiger Zahlungstermin: " + zahlungstermin);
        m.setZahlungstermin(Integer.parseInt(zahlungstermin));
      }
      else
      {
        if (m.getZahlungstermin() == null)
          m.setZahlungstermin(Zahlungstermin.MONATLICH.getKey());
      }
    }

    String mandatdatum = request.getParameter("mandatdatum");
    if (mandatdatum != null && mandatdatum.length() != 0)
    {
      m.setMandatDatum(Datum.toDate(mandatdatum));
    }

    String mandatversion = request.getParameter("mandatversion");
    if (mandatversion != null && mandatversion.length() != 0)
    {
      m.setMandatVersion(Integer.parseInt(mandatversion));
    }

    String iban = request.getParameter("iban");
    if (iban != null && iban.length() != 0)
    {
      try
      {
        IBAN i = new IBAN(iban.toUpperCase());
        m.setIban(i.getIBAN());
      }
      catch (SEPAException e)
      {
        if (e.getFehler() == SEPAException.Fehler.UNGUELTIGES_LAND)
          throw new ApplicationException(
              "IBAN Ungültiges Land: " + e.getMessage());
        else
          throw new ApplicationException(e.getMessage());
      }
    }

    String bic = request.getParameter("bic");
    if (bic != null && bic.length() != 0)
    {
      m.setBic(bic);
    }
    else
    {
      if (m.getBic() == "" && iban != null)
      {
        IBAN i = new IBAN(iban.toUpperCase());
        m.setBic(i.getBIC());
      }
    }

    String email = request.getParameter("email");
    if (email != null && email.length() != 0)
    {
      if (!EmailValidator.isValid(email))
        throw new ApplicationException("Ungültige Email: " + email);
      m.setEmail(email);
    }

    if (Einstellungen.getEinstellung().getExterneMitgliedsnummer())
    {
      String externemitgliedsnummer = request
          .getParameter("externemitgliedsnummer");
      if (externemitgliedsnummer != null
          && externemitgliedsnummer.length() != 0)
      {
        m.setExterneMitgliedsnummer(externemitgliedsnummer);
      }
      else
      {
        if (m.getExterneMitgliedsnummer() == null)
        {
          throw new ApplicationException("Externe Mitgliedsnummer fehlt");
        }
      }
    }
    else
    {
      m.setExterneMitgliedsnummer(null);
    }

    String geburtsdatum = request.getParameter("geburtsdatum");
    if (geburtsdatum != null && geburtsdatum.length() != 0)
    {
      if (Datum.toDate(geburtsdatum).after(new Date()))
        throw new ApplicationException("Geburtsdatum liegt in der Zukunft");
      m.setGeburtsdatum(Datum.toDate(geburtsdatum));
    }

    String geschlecht = request.getParameter("geschlecht");
    if (geschlecht != null && geschlecht.length() != 0)
    {
      if (!geschlecht.toLowerCase().equals("m")
          && !geschlecht.toLowerCase().equals("w")
          && !geschlecht.toLowerCase().equals("o"))
        throw new ApplicationException("Ungültiges Geschlecht: " + geschlecht);
      m.setGeschlecht(geschlecht);
    }

    String ktoiadressierungszusatz = request
        .getParameter("ktoiadressierungszusatz");
    if (ktoiadressierungszusatz != null
        && ktoiadressierungszusatz.length() != 0)
    {
      m.setKtoiAdressierungszusatz(ktoiadressierungszusatz);
    }

    String ktoianrede = request.getParameter("ktoianrede");
    if (ktoianrede != null && ktoianrede.length() != 0)
    {
      m.setKtoiAnrede(ktoianrede);
    }

    String ktoiemail = request.getParameter("ktoiemail");
    if (ktoiemail != null && ktoiemail.length() != 0)
    {
      if (!EmailValidator.isValid(ktoiemail))
        throw new ApplicationException("Ungültige Email: " + ktoiemail);
      m.setKtoiEmail(ktoiemail);
    }

    String ktoiname = request.getParameter("ktoiname");
    if (ktoiname != null && ktoiname.length() != 0)
    {
      m.setKtoiName(ktoiname);
    }

    String ktoiort = request.getParameter("ktoiort");
    if (ktoiort != null && ktoiort.length() != 0)
    {
      m.setKtoiOrt(ktoiort);
    }

    String ktoipersonenart = request.getParameter("ktoipersonenart");
    if (ktoipersonenart != null && ktoipersonenart.length() != 0)
    {
      m.setKtoiPersonenart(ktoipersonenart.substring(0, 1));
    }
    else
    {
      if (m.getPersonenart() == null)
        m.setPersonenart("N");
    }

    String ktoiplz = request.getParameter("ktoiplz");
    if (ktoiplz != null && ktoiplz.length() != 0)
    {
      m.setKtoiPlz(ktoiplz);
    }

    String ktoistaat = request.getParameter("ktoistaat");
    if (ktoistaat != null && ktoistaat.length() != 0)
    {
      m.setKtoiStaat(ktoistaat);
    }

    String ktoistrasse = request.getParameter("ktoistrasse");
    if (ktoistrasse != null && ktoistrasse.length() != 0)
    {
      m.setKtoiStrasse(ktoistrasse);
    }

    String ktoititel = request.getParameter("ktoititel");
    if (ktoititel != null && ktoititel.length() != 0)
    {
      m.setKtoiTitel(ktoititel);
    }

    String ktoivorname = request.getParameter("ktoivorname");
    if (ktoivorname != null && ktoivorname.length() != 0)
    {
      m.setKtoiVorname(ktoivorname);
    }

    String ktoigeschlecht = request.getParameter("ktoigeschlecht");
    if (ktoigeschlecht != null && ktoigeschlecht.length() != 0)
    {
      if (!ktoigeschlecht.toLowerCase().equals("m")
          && !ktoigeschlecht.toLowerCase().equals("w")
          && !ktoigeschlecht.toLowerCase().equals("o"))
        throw new ApplicationException(
            "Ungültiges Geschlecht: " + ktoigeschlecht);
      m.setKtoiGeschlecht(ktoigeschlecht);
    }

    String kuendigung = request.getParameter("kuendigung");
    if (kuendigung != null && kuendigung.length() != 0)
    {
      m.setKuendigung(kuendigung);
    }

    String sterbetag = request.getParameter("sterbetag");
    if (sterbetag != null && sterbetag.length() != 0)
    {
      m.setSterbetag(sterbetag);
    }

    String name = request.getParameter("name");
    if (name != null && name.length() != 0)
    {
      m.setName(name);
    }

    String ort = request.getParameter("ort");
    if (ort != null && ort.length() != 0)
    {
      m.setOrt(ort);
    }

    String plz = request.getParameter("plz");
    if (plz != null && plz.length() != 0)
    {
      m.setPlz(plz);
    }

    String staat = request.getParameter("staat");
    if (staat != null && staat.length() != 0)
    {
      m.setStaat(staat);
    }

    String strasse = request.getParameter("strasse");
    if (strasse != null && strasse.length() != 0)
    {
      m.setStrasse(strasse);
    }

    String telefondienstlich = request.getParameter("telefondienstlich");
    if (telefondienstlich != null && telefondienstlich.length() != 0)
    {
      m.setTelefondienstlich(telefondienstlich);
    }

    String telefonprivat = request.getParameter("telefonprivat");
    if (telefonprivat != null && telefonprivat.length() != 0)
    {
      m.setTelefonprivat(telefonprivat);
    }

    String handy = request.getParameter("handy");
    if (handy != null && handy.length() != 0)
    {
      m.setHandy(handy);
    }

    String titel = request.getParameter("titel");
    if (titel != null && titel.length() != 0)
    {
      m.setTitel(titel);
    }

    String vermerk1 = request.getParameter("vermerk1");
    if (vermerk1 != null && vermerk1.length() != 0)
    {
      m.setVermerk1(vermerk1);
    }

    String vermerk2 = request.getParameter("vermerk2");
    if (vermerk2 != null && vermerk2.length() != 0)
    {
      m.setVermerk2(vermerk2);
    }

    String vorname = request.getParameter("vorname");
    if (vorname != null && vorname.length() != 0)
    {
      m.setVorname(vorname);
    }

    if (m.getID() == null)
    {
      m.setEingabedatum();
    }

    m.setLetzteAenderung();
    m.store();
    return m;
  }

}