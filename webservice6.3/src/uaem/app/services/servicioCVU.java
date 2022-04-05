package uaem.app.services;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.apache.commons.io.FileUtils;
//import org.dspace.app.itemimport.ItemImport;
//import org.dspace.app.itemimport.ItemImportServiceImpl;
import uaem.app.itemimport.*;
import org.dspace.app.itemimport.factory.ItemImportServiceFactory;
import org.dspace.app.itemimport.factory.ItemImportServiceFactoryImpl;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ContextReadOnlyCache;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
//import org.dspace.handle.HandleManager;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import static org.joda.time.format.ISODateTimeFormat.date;
import uaem.app.webui.util.LoggerUAEM;

/**
 *
 * @author victor
 */
@WebService(serviceName = "servicioCVU")
public class servicioCVU {

	/**
	 * La operacion de depositoDocumento es la primera en relizarse, con los
	 * siguientes parametros: idCVU _ es el identificador del deposito palabrasCve _
	 * palabras que identifican el contenido del documento desc_res _ descripcion o
	 * resumen del documento titulo _ titulo del documento formato _ es la extension
	 * del archivo archivo _ son los bytes correspondientes al documento
	 */
	@WebMethod(operationName = "depositoDocumento")
	public String depositoDocumento(@WebParam(name = "idCVU") long idCVU,
			@WebParam(name = "palabrasCve") String[] palabras, @WebParam(name = "desc_res") String descripcion,
			@WebParam(name = "titulo") String titulo, @WebParam(name = "formato") String formato,
			@WebParam(name = "archivo") String archivo) {
		// EL parametro archivo se recibe como String por que nos envian una cadena en
		// base64
		// Verifica si existe archivo para crearlo o seguir escribiendo en el
		final String rutaTxtCVU = "/files/dspace/services/import/importCVU/CVUArchRecibidos.txt";
		final String rutaImportCVU = "/files/dspace/services/import/importCVU/";// ruta para archivo temporal
		boolean regAlmacenado = false;
		File archivoTxtCVU = new File(rutaTxtCVU);
		BufferedWriter bw;
		FileWriter TextOut = null;
		Date date = new Date();
		try {// registro recibido
			DateFormat hourdateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			if (archivoTxtCVU.exists()) {
				TextOut = new FileWriter(rutaTxtCVU, true);// escribe al final del archivo
				TextOut.write("idCVU" + idCVU + "::" + hourdateFormat.format(date) + "\n");
				TextOut.close();
			} else {// El fichero no existe y hay que crearlo
				TextOut = new FileWriter(rutaTxtCVU, true); // escribe al final del archivo
				TextOut.write("DEPOSITO DE DOCUMENTOS\n");
				TextOut.write("idCVU-" + idCVU + "::" + hourdateFormat.format(date) + "\n");
				TextOut.close();
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			return "falloAlEntraraAlArchivo";
		} catch (IOException ex) {
			Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			return "falloRead";
		}

		final String PATH_dirIdCVU = rutaImportCVU + "/" + idCVU;
		File folder = new File(PATH_dirIdCVU);
		if (folder.exists()) {// no deberia existir ubicacion, pues envian primero el documento
			return "El FOLIO recibido ya existe";
			/*
			 * try { FileUtils.cleanDirectory(folder); } catch (IOException ex) {
			 * Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			 * return "NO PUDO BORRARSE EL CONTENIDO EXISTENTE"; }
			 */
		} else {
			try {/* guardar archivo */
				folder.mkdirs();
				// Escribimos el titulo en el archivo contents dentro de la carpeta del idCVU
				String PathContentsTemp = PATH_dirIdCVU + "/contents";
				File archivoContens = new File(PathContentsTemp);
				TextOut = new FileWriter(archivoContens, true);// escribe al final del archivo
				TextOut.write("Titulo_" + idCVU + "." + formato);
				TextOut.close();
				String cadenaCodificada;

				/*
				 * try{ cadenaCodificada = Base64.getEncoder().encodeToString(archivo); } catch
				 * (Exception e) { // TODO: handle exception return
				 * "Error al codificar a base64"; }
				 */

				// Crea el pdf
				/*
				 * OutputStream out = null; String archivoSalida = "Titulo_" + idCVU + "." +
				 * formato; // titulo del archivo out = new FileOutputStream(PATH_dirIdCVU + "/"
				 * + archivoSalida); BufferedOutputStream outputStream = new
				 * BufferedOutputStream(out); outputStream.write(archivo); outputStream.close();
				 */

				// recibir string en base64
				String archivoSalida = "Titulo_" + idCVU + "." + formato; // titulo del archivo
				File file = new File(PATH_dirIdCVU + "/" + archivoSalida); // ruta del archivo

				try (FileOutputStream fos = new FileOutputStream(file);) {
					byte[] archivo64 = Base64.getDecoder().decode(archivo); // decodificamos el archivo
					fos.write(archivo64);// se crea el archivo en el servidor fos.close();
				} catch (Exception e) {
					e.printStackTrace();
					return
					"Error en  decodificación del archivo";
				}

				boolean banderaDirectorioT = true;
				String PATH = PATH_dirIdCVU + "/dublin_core.xml";
				String[] p = { "" };

				banderaDirectorioT = creaDC(1, idCVU, palabras, descripcion, "", "", p, p, p, "", "", "", "", "", "",
						"", "", "", "", "", "", "", PATH, "");

				////////////// valida bandera
				if (banderaDirectorioT) {
					return "Documento recibido";
				} else {
					return "Documento NO recibido";
				}

			} catch (FileNotFoundException ex) {
				Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
				return "error";
			} catch (IOException ex) {
				Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
				return "no se guardo archivo";
			}
		}
	}

	@WebMethod(operationName = "depositoMetadatos")
	public String depositoMetadatos(@WebParam(name = "idCVU") long idCVU, @WebParam(name = "autor") String[] autor,
			@WebParam(name = "colaboradorTesis") String[] colaboradorTesis,
			@WebParam(name = "colaborador") String[] colaborador, @WebParam(name = "titulo") String titulo,
			@WebParam(name = "o_titulo") String o_titulo, @WebParam(name = "fechaPublicacion") String fechaPublicacion,
			@WebParam(name = "tacceso") String tacceso, @WebParam(name = "licencia") String licencia,
			@WebParam(name = "motivoA") String motivoA, @WebParam(name = "fechaEmb") String fechaEmb,
			@WebParam(name = "editorial") String editorial, @WebParam(name = "identificadores") String identificadores,
			@WebParam(name = "tipoDoc") String tipoDoc, @WebParam(name = "nivelTesis") String nivelTesis,
			@WebParam(name = "modalidadTesis") String modalidadTesis,
			@WebParam(name = "organismoAcad") String organismoAcad, @WebParam(name = "programaEst") String programaEst,
			@WebParam(name = "ambito") String ambito, @WebParam(name = "idioma") String idioma) {
		// Verifica si existe archivo para crearlo o seguir escribiendo en el
		final String rutaTxtCVU = "/files/dspace/services/import/importCVU/archivoCVU.txt";
		final String rutaImportCVU = "/files/dspace/services/import/importCVU/";// ruta para archivo temporal
		boolean regAlmacenado = false;
		File archivoTxtCVU = new File(rutaTxtCVU);
		BufferedWriter bw;
		FileWriter TextOut = null;
		if (archivoTxtCVU.exists()) {
			try {
				/*
				 * Context c = null; Item comp = null; try { c = new Context(); comp = (Item)
				 * HandleManager.resolveToObject(c, "000");///regresa nulo si el objeto no
				 * existe } catch (SQLException ex) {
				 * Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
				 * return "falloContext"; } catch (Exception e) { comp = null; } if (comp !=
				 * null) {//valida si el handle es diferente de nulo return "1";//si el handle
				 * existio temina el proceso }
				 */
				TextOut = new FileWriter(rutaTxtCVU, true);// escribe al final del archivo
				TextOut.write("\nidCVU_" + idCVU + "_::");
				TextOut.close();
				File folder = new File(rutaImportCVU + idCVU);
				if (folder.exists()) {/* ya se recibio el documento, entonces se realiza registro de metadatos */
					regAlmacenado = creaDirectorioTemporal(idCVU, autor, colaboradorTesis, colaborador, titulo,
							o_titulo, fechaPublicacion, tacceso, licencia, motivoA, fechaEmb, editorial,
							identificadores, tipoDoc, nivelTesis, modalidadTesis, organismoAcad, programaEst, ambito,
							rutaImportCVU, idioma);
					String idCvu = "" + idCVU;
					if (regAlmacenado) {
						Boolean registroR = registro(rutaTxtCVU, idCvu, fechaEmb, motivoA);
					} else {
						return "ERROR DE REGISTRO cuando existe archivoCVU.txt en depositoMetadatos ";
					}

				} else {/* no se ha recibido documento, entonces el proceso no puede continuar */
					return "ERROR DE RECPECION. No recibi antes el documento";
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
				return "falloBArchivo";
			} catch (IOException ex) {
				Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
				return "falloRead";
			}
		} else {
			try {// El fichero no existe y hay que crearlo
				TextOut = new FileWriter(rutaTxtCVU, true); // escribe al final del archivo
				TextOut.write("Inicio de recepcion \n" + "idCVU_" + idCVU + "_::");
				TextOut.close();
				regAlmacenado = creaDirectorioTemporal(idCVU, autor, colaboradorTesis, colaborador, titulo, o_titulo,
						fechaPublicacion, tacceso, licencia, motivoA, fechaEmb, editorial, identificadores, tipoDoc,
						nivelTesis, modalidadTesis, organismoAcad, programaEst, ambito, rutaImportCVU, idioma);
				String idCvu = "" + idCVU;

				// llama proceso registro para depositar en RI
				if (regAlmacenado) {
					Boolean registroR = registro(rutaTxtCVU, idCvu, fechaEmb, motivoA);

				} else {
					return "ERROR DE REGISTRO en depositoMetadatos no existe archivoCVU.txt";
				}
			} catch (IOException ex) {
				Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		// ***************************************************************************************************
		if (regAlmacenado) {
			return "Metadatos registrados";
		} else {
			return "Metadatos NO registrados";
		}
	}

	/*
	 * Crea el directorio temporal del registro
	 */
	boolean creaDirectorioTemporal(long idCVU, String[] autor, String[] colaboradorTesis, String[] colaborador,
			String titulo, String o_titulo, String fechaPublicacion, String tacceso, String licencia, String motivoA,
			String fechaEmb, String editorial, String identificadores, String tipoDoc, String nivelTesis,
			String modalidadTesis, String organismoAcad, String programaEst, String ambito, String rutaDirectorio,
			String idioma) {
		boolean banderaDirectorioT = false;
		try {
			final String PATH_directorioTemporal = rutaDirectorio + "/" + idCVU;
			final String DC_NAME = "/dublin_core.xml";
			File folder = new File(PATH_directorioTemporal);
			if (folder.exists()) {/* Debe existir pues se creo al recibir el documento */
				/* Inicio Crea el archivo colections en el directorio temporal */
				FileOutputStream col = new FileOutputStream(PATH_directorioTemporal + "/collections");// escribe la
																										// coleccion
				String collections = "20.500.11799/95053";
				byte collectionsByte[] = collections.getBytes("UTF-8");
				col.write(collectionsByte);
				col.close();
				/* INICIO Crea el archivo DC en el directorio temporal */
				String PATH = PATH_directorioTemporal + DC_NAME;
				String[] p = { "" };
				banderaDirectorioT = creaDC(2, idCVU, p, "", titulo, o_titulo, autor, colaboradorTesis, colaborador,
						fechaPublicacion, tacceso, licencia, motivoA, fechaEmb, editorial, identificadores, tipoDoc,
						nivelTesis, modalidadTesis, organismoAcad, programaEst, ambito, PATH, idioma);
			} else {/* si no encuentra carpeta respecto a idCVU debe enviar error */
				return false;
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} catch (IOException ex) {
			Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return banderaDirectorioT;
	}

	boolean creaDC(int proceso, long idCVU, String[] palabras, String descripcion, String titulo, String o_titulo,
			String[] autor, String[] colaboradorTesis, String[] colaborador, String fechaPublicacion, String tacceso,
			String licencia, String motivoA, String fechaEmb, String editorial, String identificadores, String tipoDoc,
			String nivelTesis, String modalidadTesis, String organismoAcad, String programaEst, String ambito,
			String PATH, String idioma) {

		File f = new File(PATH);
		FileWriter fw;
		SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat formatoFecha2 = new SimpleDateFormat("yyyy-MM");
		SimpleDateFormat formatoFecha3 = new SimpleDateFormat("yyyy");

		if (proceso == 1) {// en este proceso solo se escriben palabras clave y idCVU(dc.idInterno)
							// depositodDocumento
			if (f.exists()) {// si ya existe el archivo, debe contener datos y solo se seguira escribiendo en
								// el
				return false;// no debe existir
			} else {
				try {
					fw = new FileWriter(PATH, true);
					StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					xml.append("<dublin_core>\n");
					for (String pc : palabras) {
						xml.append("<dcvalue element=\"subject\" qualifier=\"none\">" + pc + "</dcvalue>\n");
					}
					xml.append(
							"<dcvalue element=\"description\" qualifier=\"abstract\">" + descripcion + "</dcvalue>\n");
					xml.append("<dcvalue element=\"idInterno\" qualifier=\"none\">" + idCVU + "</dcvalue>\n");
					fw.write(xml.toString());
					fw.close();
					return true;
				} catch (IOException ex) {
					Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
					return false;
				}
			}

		} else {// metodo llamado por creaDirectorioTemporal
			String tipoAcceso, tlicencia, tlenguaje, tdocumento = null, modTesis = null;

			if (f.exists()) { // debe existir puesto que el metodo incial es depositoDocumento
				// FileOutputStream fos = null;
				try {
					fw = new FileWriter(PATH, true);
					// fos = new FileOutputStream(PATH);

					// Agrega titulo y titulo externo al xml
					StringBuffer xml = new StringBuffer(
							"<dcvalue element=\"title\" qualifier=\"none\">" + titulo + "</dcvalue>\n");
					xml.append("<dcvalue element=\"title\" qualifier=\"alternative\">" + o_titulo + "</dcvalue>\n");

					// Agrega los autores al xml
					for (int i = 0; i < autor.length; i++) {
						/*
						 * Si el tipo de documento es C (Capitulo de libro), entonces los autores[i] se
						 * registraran en contributor y los colaboradores[i] en contributor.author, ya
						 * que CVU los maneja al reves
						 */
						if (autor[i] != null || !"".equals(autor[i]) && tipoDoc == "C") {
							xml.append(
									"<dcvalue element=\"contributor\" qualifier=\"none\">" + autor[i] + "</dcvalue>\n");
						} else if (autor[i] != null || !"".equals(autor[i])) {
							xml.append("<dcvalue element=\"contributor\" qualifier=\"author\">" + autor[i]
									+ "</dcvalue>\n");
						}
					}

					if (colaboradorTesis.length != 0) {
						// Agrega el colaborador de la tesis que se interpreta como el tutor academico
						for (int i = 0; i < colaboradorTesis.length; i++) {
							if (colaboradorTesis[i] != null || !"".equals(colaboradorTesis[i])) {
								xml.append("<dcvalue element=\"contributor\" qualifier=\"advisor\">"
										+ colaboradorTesis[i] + "</dcvalue>\n");
							}
						}
					}

					// Agrega los colaboradores al dublin_core.xml
					for (int i = 0; i < colaborador.length; i++) {
						// SI es capitulo de libro los colaboradores se registran como autores en
						// contributor.author
						if (colaborador[i] != null || !"".equals(colaborador[i]) && tipoDoc == "C") {
							xml.append("<dcvalue element=\"contributor\" qualifier=\"author\">" + colaborador[i]
									+ "</dcvalue>\n");

						} else if (colaborador[i] != null || !"".equals(colaborador[i])) {
							xml.append("<dcvalue element=\"contributor\" qualifier=\"none\">" + colaborador[i]
									+ "</dcvalue>\n");
						}

					}

					// Validacion de fecha
					boolean fechaCorrecta = false;
					if (fechaPublicacion != null && fechaPublicacion != "") {
						try {

							formatoFecha.setLenient(false); // Para que no corrija la fecha
							formatoFecha.parse(fechaPublicacion);
							fechaCorrecta = true;
							xml.append("<dcvalue element=\"date\" qualifier=\"issued\">" + fechaPublicacion
									+ "</dcvalue>\n");
						} catch (ParseException e) {
							fechaCorrecta = false;

						}
						if (!fechaCorrecta) {
							try {
								formatoFecha2.setLenient(false); // Para que no corrija la fecha
								formatoFecha2.parse(fechaPublicacion);
								fechaCorrecta = true;
								xml.append("<dcvalue element=\"date\" qualifier=\"issued\">" + fechaPublicacion
										+ "</dcvalue>\n");
							} catch (ParseException ex) {
								fechaCorrecta = false;
							}
						}
						if (!fechaCorrecta) {
							try {
								formatoFecha3.setLenient(false); // Para que no corrija la fecha
								formatoFecha3.parse(fechaPublicacion);

								xml.append("<dcvalue element=\"date\" qualifier=\"issued\">" + fechaPublicacion
										+ "</dcvalue>\n");
							} catch (ParseException ex1) {
								return false;
							}
						}

					} else {
						return false;
					}

					/* VAlidacion de tipo de acceso */
					if (tacceso != null && tacceso != "") {
						switch (tacceso) {
						case "A":
							tipoAcceso = "openAccess";
							break;
						case "E":
							tipoAcceso = "embargoedAccess";
							break;
						case "R":
							tipoAcceso = "restrictedAccess";
							break;
						case "C":
							tipoAcceso = "closedAccess";
							break;
						default:
							tipoAcceso = "X";
							break;
						}
						if (tipoAcceso != "X") {
							xml.append("<dcvalue element=\"rights\" qualifier=\"none\">" + tipoAcceso + "</dcvalue>\n");
						}
					} else {
						return false;
					}
					/* Validacion de licencia de acceso */
					if (licencia != null && licencia != "") {
						switch (licencia) {

						case "A":
							tlicencia = "http://creativecommons.org/licenses/by/4.0";
							break;
						case "B":
							tlicencia = "http://creativecommons.org/licenses/by-nd/4.0";
							break;
						case "C":
							tlicencia = "http://creativecommons.org/licenses/by-nc-sa/4.0";
							break;
						case "D":
							tlicencia = "http://creativecommons.org/licenses/by-nc/4.0";
							break;
						case "E":
							tlicencia = "http://creativecommons.org/licenses/by-nc-nd/4.0";
							break;
						case "F":
							tlicencia = "https://creativecommons.org/licenses/by-sa/4.0";
							break;
						default:
							tlicencia = "X";
							break;
						}
						if (tlicencia != "X") {
							xml.append("<dcvalue element=\"rights\" qualifier=\"uri\">" + tlicencia + "</dcvalue>\n");
						}
					} else {
						return false;
					}
					/* fecha de embargo y motivo */

					// El motivo y fecha de embargo se agrega a dc.description para que pueda ser
					// visto por el validador. El validador
					// tendra que quitarlo de ahi y pasarlo a los campos de fecha y motivo de
					// embargo correspondientes
					if (motivoA != null && motivoA != "") {
						// Validacion de fecha: date, para la fecha de embargo
						if (fechaEmb != null && fechaEmb != "") {
							// NOs envian la fecha "1900-01-01"cuando es un documento en Acceso Abierto,
							// pero no se debe agregar a los metadatos
							if (!"1900-01-01".equals(fechaEmb)) {
								try {
									formatoFecha.setLenient(false); // Para que no corrija la fecha
									formatoFecha.parse(fechaEmb);
									fechaCorrecta = true;
									xml.append("<dcvalue element=\"description\" qualifier=\"none\">" + motivoA + ", "
											+ fechaEmb + "</dcvalue>\n");
								} catch (ParseException e) {
									fechaCorrecta = false;

								}
								if (!fechaCorrecta) {
									try {
										formatoFecha2.setLenient(false); // Para que no corrija la fecha
										formatoFecha2.parse(fechaEmb);
										fechaCorrecta = true;
										xml.append("<dcvalue element=\"description\" qualifier=\"none\">" + motivoA
												+ ", " + fechaEmb + "</dcvalue>\n");
									} catch (ParseException ex) {
										fechaCorrecta = false;
									}
								}
								if (!fechaCorrecta) {
									try {
										formatoFecha3.setLenient(false); // Para que no corrija la fecha
										formatoFecha3.parse(fechaEmb);

										xml.append("<dcvalue element=\"description\" qualifier=\"none\">" + motivoA
												+ ", " + fechaEmb + "</dcvalue>\n");
									} catch (ParseException ex1) {
										return false;
									}
								}
							} else {
								xml.append("<dcvalue element=\"description\" qualifier=\"none\">" + motivoA
										+ "</dcvalue>\n");

							}

						} else {
							xml.append(
									"<dcvalue element=\"description\" qualifier=\"none\">" + motivoA + "</dcvalue>\n");

						}

					}

					// validar si es issn o isbn
					xml.append(
							"<dcvalue element=\"identifier\" qualifier=\"isbn\">" + identificadores + "</dcvalue>\n");

					/* Validacion de tipo de documento */
					if (tipoDoc != null && tipoDoc != "") {
						switch (tipoDoc) {
						case "A":
							tdocumento = "Artículo";
							break;
						case "T":
							if (nivelTesis != null && nivelTesis != "") {
								switch (nivelTesis) {
								case "L":
									tdocumento = "Tesis de Licenciatura ";
									break;
								case "E":
									tdocumento = "Especialidad";
									break;
								case "M":
									tdocumento = "Tesis de Maestría";
									break;
								case "D":
									tdocumento = "Tesis de Doctorado";
									break;

								default:
									tdocumento = "X";
									break;
								}
							}
							if (modalidadTesis != null && modalidadTesis != "") {
								switch (modalidadTesis) {
								case "T":
									modTesis = "Tesis";
									break;
								case "S":
									modTesis = "Tesina";
									break;
								case "A":
									modTesis = "Artículo especializado para publicar en revista indizada";
									break;

								default:
									modTesis = "X";
									break;
								}
								if (modTesis != "X") {
									xml.append("<dcvalue element=\"modalidad\" qualifier=\"none\">" + modTesis
											+ "</dcvalue>\n");
								}
							}
							break;

						case "C":
							tdocumento = "Capítulo de Libro";
							break;
						case "L":
							tdocumento = "Libro";
							break;
						default:
							tdocumento = "X";
							break;
						}
						if (tdocumento != "X") {
							xml.append("<dcvalue element=\"type\" qualifier=\"none\">" + tdocumento + "</dcvalue>\n");
						}
					} else {
						return false;
					}
					// Modalidad

					// Organismo Academico
					if (organismoAcad != null && organismoAcad != "") {
						xml.append(
								"<dcvalue element=\"organismo\" qualifier=\"none\">" + organismoAcad + "</dcvalue>\n");
					}
					// Programa
					if (programaEst != null && programaEst != "") {
						xml.append("<dcvalue element=\"programa\" qualifier=\"none\">" + programaEst + "</dcvalue>\n");
					}
					// Ambito
					if (ambito != null && ambito != "") {
						xml.append("<dcvalue element=\"ambito\" qualifier=\"none\">" + ambito + "</dcvalue>\n");
					}
					// Editorial
					if (editorial != null) {
						if (!editorial.equals("-")) {
							xml.append(
									"<dcvalue element=\"publisher\" qualifier=\"none\">" + editorial + "</dcvalue>\n");
						}
					}
					/* Validacion de lenguaje */
					if (idioma != null && idioma != "") {
						switch (idioma) {
						case "E":
							tlenguaje = "spa";
							break;
						case "I":
							tlenguaje = "eng";
							break;
						case "P":
							tlenguaje = "por";
							break;
						case "A":
							tlenguaje = "deu";
							break;
						case "F":
							tlenguaje = "fra";
							break;
						case "R":
							tlenguaje = "rus";
							break;
						default:
							tlenguaje = "X";
							return false;
						}
						xml.append("<dcvalue element=\"language\" qualifier=\"iso\">" + tlenguaje + "</dcvalue>\n");

					} else {
						return false;
					}
					// Ruta y provenance
					xml.append("<dcvalue element=\"road\" qualifier=\"none\">Dorada</dcvalue>\n");
					xml.append("<dcvalue element=\"provenance\" qualifier=\"none\">Científica</dcvalue>\n");
					xml.append("</dublin_core>\n");

					// Escribimos los metadatos en dublin_core.xml
					fw.write(xml.toString());
					fw.close();
					return true;
				} catch (FileNotFoundException ex) {
					Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					// fos.close();
				}
			} else {
				return false;
			}
		}
		return false;
	}

	boolean registro(String rutaArchivo, String idcvu, String fechaEmbargo, String motivoEmbargo) {
		HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
		EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();
		ContextReadOnlyCache readOnlyCache = new ContextReadOnlyCache();

		Context c = null;
		Item comp = null;
		EPerson myEPerson = null;
		final String MAP_FILE = "/files/dspace/services/import/importCVU/" + idcvu + "/mapfile";
		final String RutaReg = "/files/dspace/services/import/importCVU/" + idcvu;
		final String PATH = "/files/dspace/services/import/importCVU";
		final String PATH2 = idcvu;

		ItemImportServiceImpl importar = new ItemImportServiceImpl();
		try {
			c = new Context();
			comp = (Item) handleService.resolveToObject(c, "1");/// regresa nulo si el objeto no existe
			if (comp != null) {// valida si el handle es difernte de nulo
				return false;// si el handle existio temina el proceso
			}

			myEPerson = personService.findByEmail(c, "depositasiea@gmail.com");
			c.setCurrentUser(myEPerson);

			File outFile = new File(MAP_FILE);
			PrintWriter mapOut = new PrintWriter(new FileWriter(outFile));

			List<Collection> mycollections = new ArrayList<>();

			try {
				Collection mycollection = (Collection) handleService.resolveToObject(c, "20.500.11799/95053");
				mycollections.add(mycollection);
			} catch (SQLException e) {
				mapOut.close();
				return false;
			}

			// Collection[] mycollections = {(Collection) handleService.resolveToObject(c,
			// "20.500.11799/95053")};
			// c.restoreAuthSystemState();
			c.turnOffAuthorisationSystem();

			Item item = importar.addItem(c, mycollections, PATH, PATH2, mapOut, false);

			// File archivoReg = new File(rutaArchivo);
			// FileWriter TextOut = new FileWriter(archivoReg, true);//escribe al final del
			// archivo
			// String nomTxtComp = item.getHandle();
			// TextOut.write("----handle::" + nomTxtComp);
			// TextOut.close();
			File folder = new File(RutaReg);
			FileUtils.cleanDirectory(folder);
			FileUtils.deleteDirectory(folder);

			/* Aqui se comienza el registro del deposito en el archivo csv de registro */
			final String nombreDeArchivo = PATH + "/relacionRegistros.csv";
			crearArchivoCSV(nombreDeArchivo, ";", idcvu, fechaEmbargo, motivoEmbargo);

			readOnlyCache.clear();
			if (mapOut != null) {
				mapOut.flush();
				mapOut.close();
			}
			c.complete();

		} catch (SQLException ex) {
			Logger.getLogger(servicios.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} catch (IOException ex) {
			Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} catch (AuthorizeException ex) {
			Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} catch (Exception ex) {
			Logger.getLogger(servicioCVU.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}

		return true;
	}

	private static void crearArchivoCSV(String file, String delim, String idCVU, String fechaEmbargo,
			String motivoEmbargo) {
		final String NEXT_LINE = "\n";
		try {
			Date date = new Date();
			DateFormat hourdateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			FileWriter fw;
			File f = new File(file);

			if (f.exists()) {// si ya existe el archivo, debe contener datos y solo se seguira escribiendo en
								// el
				fw = new FileWriter(file, true);// escribe al final del archivo
				if ((fechaEmbargo == null || fechaEmbargo.equals(""))
						|| fechaEmbargo.equals("1900-01-01") && (motivoEmbargo == null || motivoEmbargo.equals(""))) {
					fw.write("" + idCVU + ";" + hourdateFormat.format(date) + ";V;NA;;;;;\n");
				} else {
					fw.write("" + idCVU + ";" + hourdateFormat.format(date) + ";V;NA;;;" + fechaEmbargo + ";"
							+ motivoEmbargo + ";\n");
				}
				fw.close();
			} else {// El fichero no existe y hay que crearlo
				fw = new FileWriter(file, true); // escribe al final del archivo
				fw.append("idCVU").append(delim);
				fw.append("Fecha de recepcion").append(delim);
				fw.append("Estado de deposito").append(delim);
				fw.append("URL").append(delim);
				fw.append("Fecha de validacion").append(delim);
				fw.append("Observaciones").append(delim);
				fw.append("Fecha de embargo").append(delim);
				fw.append("Motivo de embargo").append(NEXT_LINE);

				if ((fechaEmbargo == null || fechaEmbargo.equals(""))
						|| fechaEmbargo.equals("1900-01-01") && (motivoEmbargo == null || motivoEmbargo.equals(""))) {
					fw.write("" + idCVU + ";" + hourdateFormat.format(date) + ";V;NA;;;;;\n");
				} else {
					fw.write("" + idCVU + ";" + hourdateFormat.format(date) + ";V;NA;;;" + fechaEmbargo + ";"
							+ motivoEmbargo + ";\n");
				}

			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// Error al crear el archivo, por ejemplo, el archivo
			// está actualmente abierto.
			e.printStackTrace();
		}
	}

	@WebMethod(operationName = "consultaRegistro")
	public String[] consultaRegistroIdCVU(@WebParam(name = "idCVU") long idCVU) {
		// Verifica si existe archivo para crearlo o seguir escribiendo en el
		final String PATH = "/files/dspace/services/import/importCVU//relacionRegistros.csv";
		final String SEPARATOR = ";";
		String[] arregloRetorno = { "", "", "", "", "", "", "", "" };

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(PATH));
			String line = br.readLine();
			while (null != line) {
				String[] fields = line.split(SEPARATOR);
				String idCVUstr = Long.toString(idCVU);
				if (fields[0].toString().equals(idCVUstr)) {
					for (int i = 0; i < 8; i++) {
						arregloRetorno[i] = fields[i];
					}

					return arregloRetorno;
				}
				line = br.readLine();
			}
			br.close();
			return arregloRetorno;
		} catch (Exception e) {
			return arregloRetorno;
		}
	}
}