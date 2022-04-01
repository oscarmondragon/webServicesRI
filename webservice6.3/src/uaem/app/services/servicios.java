/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uaem.app.services;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.apache.commons.io.FileUtils;
//import org.dspace.app.itemimport.ItemImport;
//import uaem.app.itemimport.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ContextReadOnlyCache;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import uaem.app.itemimport.ItemImportServiceImpl;
import uaem.app.webui.util.LoggerUAEM;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.dspace.authorize.AuthorizeException;

/**
 * .
 * @author victor
 */
@WebService(serviceName = "servicios")
public class servicios {

    private ItemImportServiceImpl importar;

	@WebMethod(operationName = "deposito")
    public String deposito(
            @WebParam(name = "id") long id,
            @WebParam(name = "titulo") String titulo,
            @WebParam(name = "autor") String[] autor,
            @WebParam(name = "fecha") String fecha,
            @WebParam(name = "editorial") String editorial,
            @WebParam(name = "palabras") String[] palabras,
            @WebParam(name = "descripcion") String descripcion,
            @WebParam(name = "tipo") String tipo,
            @WebParam(name = "item") String itemFin,
            @WebParam(name = "formato") String formato,
            @WebParam(name = "archivo") byte[] archivo) {

        //Verifica si existe archivo para crearlo o seguir escribiendo en el
        final String PathArchivoSecme = "/files/dspace/services/import/2019/archivoSecme.txt";
        final String rutaImport = "/files/dspace/services/import/2019/";//ruta para archivo temporal
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();
        ContextReadOnlyCache readOnlyCache = new ContextReadOnlyCache();
        boolean regAlmacenado = false;
        String folioAnterior, itemFinT, tituloAnt;
        File archivoTxt = new File(PathArchivoSecme);///files/dspace/services/import/archivoSecme.txt..utlimo registro
        BufferedWriter bw;
        String PathFileTemp = "/files/dspace/services/import/2019/registros";//..todos los registros
        File archivoReg = new File(PathFileTemp);
        if (archivoTxt.exists()) {//guarda el dato del ultimo registro recibido id,nombre
            try {
                FileReader f = new FileReader(PathArchivoSecme);//lee archivopSecme.txt
                BufferedReader b = new BufferedReader(f);

                while ((folioAnterior = b.readLine()) != null) {//se extrae el folio escrito del registro anterior
                    tituloAnt = b.readLine();
                    if (id == -1) {//Folio id = -1, termina el proceso de envio por parte de DEP
                        itemFinT = b.readLine();
                        Context c = null;
                        Item comp = null;
                        try {
                            c = new Context();
                            comp = (Item) handleService.resolveToObject(c, itemFinT);///regresa nulo si el objeto no existe
                        } catch (SQLException ex) {
                            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                            return "falloContext";
                        } catch (Exception e) {
                            comp = null;
                        }
                        if (comp != null) {//valida si el handle es difernte de nulo
                            return "1";//si el handle existio temina el proceso
                        }

                        FileWriter TextOut = new FileWriter(archivoReg, true);//escribe al final del archivo de registros
                        TextOut.write("\nID-1 " + id + " registraultimoid " + folioAnterior);
                        TextOut.close();//

                        regAlmacenado = registraItem(folioAnterior, itemFinT, rutaImport + "directorioTemporal", c, comp);
                        break;
                    } else if ((Long.parseLong(folioAnterior) == id) && (tituloAnt.equalsIgnoreCase(titulo) )) {//si es el mismo idFolio
                        regAlmacenado = archivoTxtComp(id, archivo, formato, rutaImport);
                        FileWriter TextOut = new FileWriter(archivoReg, true);//escribe al final del archivo
                        TextOut.write("\nID llegada igual " + id + " ultimo ID " + folioAnterior);
                        TextOut.close();
                        break;
                    } else {
                        itemFinT = b.readLine();
                        Context c = null;
                        Item comp = null;
                        try {
                            c = new Context();
                            comp = (Item) handleService.resolveToObject(c, itemFinT);///regresa nulo si el objeto no existe
                        } catch (SQLException ex) {
                            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                            return "falloContext";
                        } catch (Exception e) {
                            comp = null;
                        }
                        if (comp != null) {//valida si el handle es difernte de nulo
                            return "1";//si el handle existio temina el proceso
                        }
                        FileWriter TextOut = new FileWriter(archivoReg, true);//escribe al final del archivo
                        TextOut.write("\nID diferente " + id + " id a registrar " + folioAnterior); 
                        TextOut.write("\ntitulo recibido::" +  titulo+ "::titulo a reghistrar::" + tituloAnt+"::");
                        TextOut.close();
                        regAlmacenado = registraItem(folioAnterior, itemFinT, rutaImport + "directorioTemporal", c, comp);

                        File folder = new File(rutaImport + "directorioTemporal");
                        if (folder.exists()) {
                            try {
                                folder.setExecutable(true, false);
                                folder.setWritable(true, false);
                                folder.setReadable(true, false);
                                FileUtils.cleanDirectory(folder);
                            } catch (IOException ex) {
                                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                                return "falloClean";
                            }

                        } else {
                            folder.mkdirs();
                        }
                        regAlmacenado = creaDirectorioTemporal(id, titulo, autor,
                                fecha, editorial, palabras, descripcion, tipo, itemFin,
                                formato, archivo, rutaImport);
                        break;
                    }
                }
                b.close();
                //    bw = new BufferedWriter(new FileWriter(archivoTxt));
                //  bw.write(id + "");
                // bw.close();
                f.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                return "falloBArchivo";
            } catch (IOException ex) {
                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                return "falloRead";
            }
        } else {            FileWriter TextOut = null;
            try {
                // El fichero no existe y hay que crearlo
                TextOut = new FileWriter(archivoReg, true); //escribe al final del archivo
                TextOut.write("::"+titulo+"::primer registro " + id +"\n");
                TextOut.close();
                regAlmacenado = creaDirectorioTemporal(id, titulo, autor,
                        fecha, editorial, palabras, descripcion, tipo, itemFin,
                        formato, archivo, rutaImport);
                
            } catch (IOException ex) {
                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);                
            } 
        }
        //***************************************************************************************************
        if (regAlmacenado) {
            return "registrado";
        } else {
            return "NO registrado";
        }

    }

    boolean registraItem(String folioAnterior, String itemFinT, String rutaTemp, Context c, Item comp) {
        try {
            String PathFileTemp = "/files/dspace/services/import/2019/registros";
            File archivoTxt = new File(PathFileTemp);
            FileWriter TextOut = new FileWriter(archivoTxt, true);//escribe al final del archivo
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
            EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();
            ContextReadOnlyCache readOnlyCache = new ContextReadOnlyCache();
            if (archivoTxt.exists()) {
                TextOut.write("\n Entra a registrar - " + folioAnterior);
            } else {//si el archivo contents no existe
                TextOut.write("\nEntra a registrar PRIMER registro - " + folioAnterior);
            }
            TextOut.close();//
            importar = null;
            //ItemImport importar = new ItemImport();
            EPerson myEPerson = null;
            Random r = new Random();
            OutputStream out = null;
            final String PATH = "/files/dspace/services/import/2019";
            final String PATH_RAMDOM = "import_" + r.nextLong() * 1000000;
            final String NAME_ITEM = "item_2016";
            final String DC_NAME = "/dublin_core.xml";
            final String MAP_FILE = "/files/dspace/services/import/2019/" + PATH_RAMDOM + "/item_2016/mapfile";

            LoggerUAEM logUAEM = null;
            try {
                logUAEM = new LoggerUAEM(PATH);
            } catch (IOException ex) {
                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            }
            org.apache.log4j.Logger log = logUAEM.getLog();

            File folder = new File(PATH + "/" + PATH_RAMDOM + "/" + NAME_ITEM);
            if (folder.exists()) {
                try {
                    folder.setExecutable(true, false);
                    folder.setWritable(true, false);
                    folder.setReadable(true, false);
                    FileUtils.cleanDirectory(folder);
                } catch (IOException ex) {
                    log.warn(ex.getMessage());
                    Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                folder.mkdirs();
            }
            TextOut = new FileWriter(archivoTxt, true);//escribe al final del archivo
            TextOut.write("----random::" + PATH_RAMDOM);
            TextOut.close();//
            // Creacion de archivos
            //trae la lista de nombres de archivos
            String[] dirListing = new File(rutaTemp).list();
            int numeroArchivos = dirListing.length, i = 0;
            String nombresArchivos[] = new String[numeroArchivos];
            TextOut = new FileWriter(archivoTxt, true);//escribe al final del archivo            
            for (String fileName : dirListing) {
                if ("dublin_core.xml".equals(fileName) || "contents".equals(fileName) || "collections".equals(fileName)) {
                    TextOut.write("----dcc::" + fileName);
                } else {
                    TextOut.write("----arch::" + fileName + i);
                    nombresArchivos[i] = fileName;
                    i++;
                }
            }
            TextOut.close();
            String fromFile, toFile;
            boolean result;
            ///crear archivo DC en directorioTemporal
            fromFile = rutaTemp + "/dublin_core.xml";
            toFile = PATH + "/" + PATH_RAMDOM + "/" + NAME_ITEM + "/dublin_core.xml";
            result = copyFile(fromFile, toFile);
            if (result == false) {
                return false;
            }

            ///crear archivo contents en directorioTemporal
            fromFile = rutaTemp + "/contents";
            toFile = PATH + "/" + PATH_RAMDOM + "/" + NAME_ITEM + "/contents";
            result = copyFile(fromFile, toFile);
            if (result == false) {
                return false;
            }

            ///crear archivo colections en directorioTemporal
            fromFile = rutaTemp + "/collections";
            toFile = PATH + "/" + PATH_RAMDOM + "/" + NAME_ITEM + "/collections";
            result = copyFile(fromFile, toFile);
            if (result == false) {
                return false;
            }
            ///crear archivos texto completo en directorioTemporal
            for (String fileName : dirListing) {
                fromFile = rutaTemp + "/" + fileName;
                toFile = PATH + "/" + PATH_RAMDOM + "/" + NAME_ITEM + "/" + fileName;
                result = copyFile(fromFile, toFile);
                if (result == false) {
                    return false;
                }
            }
            // Fin de ceración de archivo
            myEPerson = personService.findByEmail(c, "ri@uaemex.mx");
            c.setCurrentUser(myEPerson);

            File outFile = new File(MAP_FILE);
            PrintWriter mapOut = new PrintWriter(new FileWriter(outFile));

            //Collection[] mycollections = {(Collection) handleService.resolveToObject(c, "20.500.11799/107520")};
            
            
            List<Collection> mycollections = new ArrayList<>();
            String nomTxtComp;
            
            try {
				 Collection mycollection = (Collection) handleService.resolveToObject(c, "20.500.11799/107520");
				 mycollections.add(mycollection);
				 c.turnOffAuthorisationSystem();
		         Item item = importar.addItem(c, mycollections, PATH + "/" + PATH_RAMDOM, NAME_ITEM, mapOut, false);
		         
		         readOnlyCache.clear();
		            
		            if (mapOut != null) {
		                mapOut.flush();
		                mapOut.close();
		            }
		            c.complete();

		            nomTxtComp = item.getHandle();
			 }catch (SQLException e) {  
				 mapOut.close();
                return false;
            }
            
           
            
           
            TextOut = new FileWriter(archivoTxt, true);//escribe al final del archivo
            TextOut.write("----handle::" + nomTxtComp);
            TextOut.close();
            
            PathFileTemp = "/files/dspace/services/import/2019/registro_ID_HANDLE";
            File archivoRegistos = new File(PathFileTemp);
            TextOut = new FileWriter(archivoRegistos, true);//escribe al final del archivo
            TextOut.write(folioAnterior + " ::" + nomTxtComp + "\n");
            TextOut.close();            
            
            FileUtils.cleanDirectory(folder);
             FileUtils.deleteDirectory(folder);
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (AuthorizeException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (Exception ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    boolean copyFile(String fromFile, String toFile) {
        File origin = new File(fromFile);
        File destination = new File(toFile);
        if (origin.exists()) {
            try {
                InputStream in = new FileInputStream(origin);
                OutputStream out = new FileOutputStream(destination);
                // We use a buffer for the copy (Usamos un buffer para la copia).
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();

                out.close();

                return true;
            } catch (IOException ioe) {
                Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ioe);
                ioe.printStackTrace();
                return false;
            }
        } else {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, "No existio el archivo de origen");
            return false;
        }
    }

    boolean creaDirectorioTemporal(long id, String titulo, String[] autor,
            String fecha, String editorial, String[] palabras,
            String descripcion, String tipo, String itemFin,
            String formato, byte[] archivo, String ruta
    ) {
        boolean banderaDirectorioT = false;
        try {
            /*  INICIO Crea el directorio temporal   */
            final String PATH_directorioTemporal = ruta + "/directorioTemporal";
            final String DC_NAME = "/dublin_core.xml";
            File folder = new File(PATH_directorioTemporal);
            if (folder.exists()) {
                try {
                    folder.setExecutable(true, false);
                    folder.setReadable(true, false);
                    folder.setWritable(true, false);
                    FileUtils.cleanDirectory(folder);
                } catch (IOException ex) {
                    Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            } else {
                folder.mkdirs();
            }//
            /*  INICIO Crea el archivo contents y archivoDeContenido(secme-id-formato) en el directorio temporal   */
            banderaDirectorioT = archivoTxtComp(id, archivo, formato, ruta);//
            if (banderaDirectorioT == false) {
                return false;
            }
            /*Inicio Crea el archivo colections en el directorio temporal*/
            FileOutputStream col = new FileOutputStream(PATH_directorioTemporal + "/collections");//escribe la coleccion
            String collections = "20.500.11799/107520";
            byte collectionsByte[] = collections.getBytes("UTF-8");
            col.write(collectionsByte);
            col.close();
            /*  INICIO Crea el archivo DC en el directorio temporal   */
            FileOutputStream fos = new FileOutputStream(PATH_directorioTemporal + DC_NAME);
            StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<dublin_core>\n");
            xml.append("<dcvalue element=\"title\" qualifier=\"none\">" + titulo + "</dcvalue>\n");
            for (String a : autor) {
                if (a.contains(",")) {
                    String au[] = a.split(",");
                    for (String a2 : au) {
                        xml.append("<dcvalue element=\"contributor\" qualifier=\"author\">" + a2 + "</dcvalue>\n");
                    }
                } else {
                    xml.append("<dcvalue element=\"contributor\" qualifier=\"author\">" + a + "</dcvalue>\n");

                }
            }
            xml.append("<dcvalue element=\"date\" qualifier=\"issued\">" + fecha + "</dcvalue>\n");
            if (editorial != null) {
                if (!editorial.equals("-")) {
                    xml.append("<dcvalue element=\"publisher\" qualifier=\"none\">" + editorial + "</dcvalue>\n");

                }
            }
            if (palabras != null) {
                for (String p : palabras) {
                    if (!p.equals("-")) {
                        xml.append("<dcvalue element=\"subject\" qualifier=\"none\">" + p + "</dcvalue>\n");

                    }
                }
            }
            if (descripcion != null) {
                xml.append("<dcvalue element=\"description\" qualifier=\"none\">" + descripcion + "</dcvalue>\n");
            }
            xml.append("<dcvalue element=\"type\" qualifier=\"none\">" + tipo + "</dcvalue>\n");
            xml.append("<dcvalue element=\"language\" qualifier=\"none\">es</dcvalue>\n");
            xml.append("<dcvalue element=\"road\" qualifier=\"none\">Dorada</dcvalue>\n");
            xml.append("<dcvalue element=\"provenance\" qualifier=\"none\">Académica</dcvalue>\n");
            xml.append("</dublin_core>\n");
            byte codigos[] = xml.toString().getBytes("UTF-8");
            fos.write(codigos);
            fos.close();//
            /*Llena archivo temporal archivoSecme.txt que guarda folio y itemFin*/
            banderaDirectorioT = llenaArchivoTemporal(id, itemFin,titulo,ruta);
            //
        } catch (FileNotFoundException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return banderaDirectorioT;
    }

    boolean archivoTxtComp(long id, byte[] archivo, String formato, String ruta) {
        //"/files/dspace/services/import/directorioTemporal"
        String PathContentsTemp = ruta + "directorioTemporal/contents";
        String nomTxtComp = "";
        int i;
        try {
            File archivoContens = new File(PathContentsTemp);
            if (archivoContens.exists()) {
                /* INICIO  Contabilizamos los archivos existentes en contents*/
                String linea;
                FileReader f = new FileReader(PathContentsTemp);
                BufferedReader b = new BufferedReader(f);
                i = 1;
                while ((linea = b.readLine()) != null) {//se extrae el folio escrito del registro anterior
                    i++;
                }
                b.close();//
                /*INICIO crear archivo  texto completo*/
                nomTxtComp = creaTxtCompleto(id, formato, ruta + "directorioTemporal", archivo, i);//
                /*Inicio seguir escribiendo en el contents*/
                FileWriter TextOut = new FileWriter(archivoContens, true);//escribe al final del archivo
                TextOut.write("\n" + nomTxtComp);
                TextOut.close();//
            } else {//si el archivo contents no existe
                i = 1;
                /*INICIO crear archivo  texto completo*/
                nomTxtComp = creaTxtCompleto(id, formato, ruta + "directorioTemporal", archivo, i);//
                /*Inicio seguir escribiendo en el contents*/
                FileWriter TextOut = new FileWriter(archivoContens, true);//escribe al final del archivo
                TextOut.write(nomTxtComp);
                TextOut.close();//
            }
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    String creaTxtCompleto(long id, String formato, String ruta, byte[] archivo, int i) {
        try {
            OutputStream out = null;
            String archivoSalida = "secme-" + id + "_" + i + formato;
            out = new FileOutputStream(ruta + "/" + archivoSalida);
            BufferedOutputStream outputStream = new BufferedOutputStream(out);
            outputStream.write(archivo);
            outputStream.close();
            return archivoSalida;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    boolean llenaArchivoTemporal(long id, String itemFin,String titulo, String ruta) {
        FileOutputStream fos = null;
        File farchivoSecme = new File(ruta + "archivoSecme.txt");
        try {
            if (farchivoSecme.exists()) {

                farchivoSecme.setExecutable(true, false);
                farchivoSecme.setReadable(true, false);
                farchivoSecme.setWritable(true, false);
                farchivoSecme.delete();
            }
            /*INICIO...Llenado de archivo temporal*/
            fos = new FileOutputStream(ruta + "archivoSecme.txt");

            String texto = id + "\n" + titulo + "\n";  //ID
            texto += itemFin;
            byte codigos[] = texto.getBytes("UTF-8");
            fos.write(codigos);
            fos.close();//
            return true;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

  
}