/*******************************************************************************
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un aplicativo de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde http://forja-ctt.administracionelectronica.gob.es/
 * Copyright 2009,2010,2011 Gobierno de Espana
 * Este fichero se distribuye bajo  bajo licencia GPL version 2  segun las
 * condiciones que figuran en el fichero 'licence' que se acompana. Si se distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 ******************************************************************************/

package es.gob.afirma.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/** Funciones de utilidad para la instalaci&oacute;n del Cliente Afirma. */
final class AOInstallUtils {

    /** Gestor de registro. */
    private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$;

    private AOInstallUtils() {
        // No permitimos la instanciacion
    }

    private static final int BUFFER_SIZE = 1024;

    static final String PACK200_SUFIX = ".pack.gz"; //$NON-NLS-1$
    private static final String JAR_SUFIX = ".jar"; //$NON-NLS-1$


    /** Desempaqueta un fichero Pack200 para obtener el fichero JAR equivalente. El fichero
     * JAR resultante se almacenar&aacute; en el mismo directorio que el original y tendr&aacute;
     * por nombre el mismo sin la extension ".pack.gz" o ".pack" y terminado en ".jar".
     * @param pack200Filename Nombre del del fichero Pack200 origen, incluyendo ruta
     * @throws URISyntaxException Cuando la ruta del fichero no es v&aacute;lida.
     * @throws IOException Cuando no se puede cargar o descomprimir el fichero. */
    static void unpack(final String pack200Filename) throws IOException, URISyntaxException {

        // Obtenemos el nombre del fichero de salida
        String jarFilename = pack200Filename + JAR_SUFIX;
        if (pack200Filename.endsWith(".pack") || pack200Filename.endsWith(PACK200_SUFIX)) { //$NON-NLS-1$
            jarFilename = pack200Filename.substring(0, pack200Filename.lastIndexOf(".pack")); //$NON-NLS-1$
            if (!jarFilename.endsWith(JAR_SUFIX)) {
                jarFilename += JAR_SUFIX;
            }
        }

        // Desempaquetamos
        unpack(pack200Filename, jarFilename);
    }

    /** Desempaqueta un fichero Pack200 para obtener el fichero JAR equivalente.
     * @param pack200Filename Nombre del del fichero Pack200 origen, incluyendo ruta
     * @param targetJarFilename Nombre del fichero de salida, incluyendo ruta
     * @throws URISyntaxException Cuando la ruta del fichero no es v&aacute;lida.
     * @throws IOException Cuando no se ha podido cargar o descomprimir el recurso. */
    static void unpack(final String pack200Filename, final String targetJarFilename) throws IOException, URISyntaxException {
        if (pack200Filename == null) {
            throw new IllegalArgumentException("El Pack200 origen no puede ser nulo"); //$NON-NLS-1$
        }
        if (targetJarFilename == null) {
            throw new IllegalArgumentException("El JAR de destino no puede ser nulo"); //$NON-NLS-1$
        }
        createDirectory(new File(targetJarFilename).getParentFile());
        unpack200gunzip(AOBootUtil.loadFile(AOBootUtil.createURI(pack200Filename)),
                            new FileOutputStream(new File(targetJarFilename)));

        // Eliminamos la version empaquetada
        new File(pack200Filename).delete();

    }

    /** Descomprime un JAR comprimido con Pack200 y GZip.
     * @param packgz <i>jar.pack.gz</i> original
     * @param jar JAR resultante del desempaquetado
     * @throws IOException Si ocurre cualquier problema durante la descompresi&oacute;n */
    private static void unpack200gunzip(final InputStream packgz, final OutputStream jar) throws IOException {
        if (packgz == null) {
            throw new IllegalArgumentException("El pack.gz es nulo"); //$NON-NLS-1$
        }
        final InputStream is = new GZIPInputStream(packgz);
        final Unpacker u = java.util.jar.Pack200.newUnpacker();
        final JarOutputStream jos = new JarOutputStream(jar);
        u.unpack(is, jos);
        jos.flush();
        jos.close();
    }

    /** Descomprime un fichero Zip en un directorio. Si el directorio de destino no existe,
     * se crear&aacute;. En caso de ocurrir un error durante la descompresi&oacute;n de un
     * fichero concreto, la operaci&oacute;n no se detendr&aacute; y se tratar&aacute; de
     * descomprimir el resto.<br>
     * Si se introduce un directorio nulo o vac&iacute;o se descomprimir&aacute; en el directorio
     * actual.
     * @param zipFile Fichero zip
     * @param destDirectory Ruta del directorio en donde deseamos descomprimir
     * @throws IOException El nombre de directorio indicado coincide con el de un fichero */
    private static void unzip(final ZipFile zipFile, final File destDirectory) throws IOException {

        if (zipFile == null) {
            throw new IllegalArgumentException("El fichero Zip no puede ser nulo"); //$NON-NLS-1$
        }
        if (destDirectory == null) {
            throw new IllegalArgumentException("El directorio de destino para descomprimir el Zip no puede ser nulo"); //$NON-NLS-1$
        }

        // Si no existe el directorio de destino, lo creamos
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }
        else if (destDirectory.isFile()) {
            throw new IOException("Ya existe un fichero con el nombre indicado para el directorio de salida"); //$NON-NLS-1$
        }
        else if (!destDirectory.canRead()) {
            throw new IOException("No se dispone de permisos suficientes para almacenar ficheros en el directorio destino: " + destDirectory); //$NON-NLS-1$
        }

        // Descomprimimos los ficheros
        final byte[] buffer = new byte[BUFFER_SIZE];
        String entryName;
        File outputFile;
        InputStream zeis;
        FileOutputStream fos;
        int nBytes;
        for (final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries(); zipEntries.hasMoreElements();) {
            final ZipEntry entry = zipEntries.nextElement();
            try {
                entryName = entry.getName();

                // No decomprimimos entradas con "..", para evitar problemas de seguridad
                if (entryName.contains("..")) { //$NON-NLS-1$
                    continue;
                }

                // Por motivos de seguridad nunca descomprimimos los elementos relacionados con
                // firmas JAR
                if (AOJarVerifier.signatureRelated(entryName)) {
                    continue;
                }

                outputFile = new File(destDirectory, entryName);
                if (entry.isDirectory()) {
                    outputFile.mkdir();
                }
                else {
                    // Creamos el arbol de directorios para el fichero
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }

                    // Descomprimimos el fichero
                    zeis = zipFile.getInputStream(entry);
                    fos = new FileOutputStream(outputFile);
                    while ((nBytes = zeis.read(buffer)) != -1) {
                        fos.write(buffer, 0, nBytes);
                    }
                    try {
                        fos.flush();
                    }
                    catch (final Exception e) {
                        // Ignoramos los errores en el vaciado
                    }
                    try {
                        fos.close();
                    }
                    catch (final Exception e) {
                        // Ignoramos los errores en el cierre
                    }
                }
            }
            catch (final Exception e) {
                throw new IOException("Error durante la instalacion, no se pudo instalar la biblioteca '" //$NON-NLS-1$
                                      + entry.getName()
                                      + "': " + e //$NON-NLS-1$
                );
            }
        }
    }

    /** Copia un fichero indicado por una URL en un directorio local del sistema.
     * @param file Fichero que se desea copiar.
     * @param fileDest Fichero de destino.
     * @throws URISyntaxException Si la URI proporcionada no tiene una sintaxis v&aacute;lifa
     * @throws IOException Si ocurre un error de entrada/salida */
    static void copyFileFromURL(final URL file, final File fileDest) throws IOException, URISyntaxException {
        copyFileFromURL(file, fileDest.getParentFile(), fileDest.getName());
    }

    /** Copia un fichero referenciado por una URL en un directorio local del sistema.
     * @param urlFile Fichero que se desea copiar.
     * @param dirDest Directorio local.
     * @param newFilename Nombre con el que se almacenar&aacute; el fichero. Si se indica <code>null</code> se almacena con el mismo nombre que
     *        tuviese el fichero remoto.
     * @throws IOException Si ocurren errores de entrada/salida
     * @throws URISyntaxException Si la URO proporcionada no tiene una sintaxis v&aacute;lida */
    private static void copyFileFromURL(final URL urlFile, final File dirDest, final String newFilename) throws IOException, URISyntaxException {

        if (urlFile == null) {
            throw new IllegalArgumentException("La URL al fichero remoto no puede ser nula"); //$NON-NLS-1$
        }
        if (dirDest == null) {
            throw new IllegalArgumentException("El directorio destino no puede ser nulo"); //$NON-NLS-1$
        }

        // Obtenemos el nombre del fichero destino a partir del directorio de destino y el nombre del nuevo
        // fichero o el del fichero remoto si no se indico uno nuevo
        final String filename = (newFilename != null ? newFilename : urlFile.getPath().substring(urlFile.getPath().lastIndexOf('/')));
        final File outFile = new File(dirDest, filename);
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        else if (outFile.getParentFile().isFile()) {
            throw new IOException("No se ha podido crear el arbol de directorios necesario"); //$NON-NLS-1$
        }

        InputStream is = AOBootUtil.loadFile(new URI(urlFile.toString()));
        final URI uri = AOBootUtil.createURI(urlFile.toString());
        if (uri.getScheme().equalsIgnoreCase("file")) { //$NON-NLS-1$
            try {
                is = new FileInputStream(urlFile.toString().substring(7));
            }
            catch (final Exception e) {
                is = uri.toURL().openStream();
            }
        }
        else {
            is = uri.toURL().openStream();
        }

        int nBytes = 0;
        final byte[] buffer = new byte[BUFFER_SIZE];
        final FileOutputStream fos = new FileOutputStream(outFile);
        while ((nBytes = is.read(buffer)) != -1) {
            fos.write(buffer, 0, nBytes);
        }

        try {
            is.close();
        }
        catch (final Exception e) {
            LOGGER.warning("No se pudo cerrar el fichero remoto: " + e); //$NON-NLS-1$
        }
        try {
            fos.close();
        }
        catch (final Exception e) {
            LOGGER.warning("No se pudo cerrar el fichero local: " + e); //$NON-NLS-1$
        }
    }

    /** Crea un fichero vac&iacute;o temporal.
     * @return Fichero vac&iacute;o creado. */
    static File createTempFile() {
        return createTempFile(false);
    }

    /** Crea un fichero vac&iacute;o o un directorio temporal.
     * @param isDir Indica si debe crearse un directorio ({@code true}) o un fichero ({@code false}).
     * @return Directorio/Fichero creado. */
    static File createTempFile(final boolean isDir) {
        int n = 0;
        File file = null;
        do {
            try {
                file = File.createTempFile("afirma", null); //$NON-NLS-1$
            }
            catch (final Exception e) {
                continue;
            }
            if (file != null && file.exists() && file.isFile()) {
                file.delete();
            }
            n++;
        } while (file != null && file.exists() && n <= 5);

        if (n > 5 || file == null) {
            LOGGER.warning("No se pudo crear un fichero temporal"); //$NON-NLS-1$
            return null;
        }

        if (isDir) {
            file.mkdir();
        }

        return file;
    }

    /** Borra un directorio y todos sus subdirectorios y ficheros.
     * @param dir Directorio a borrar.
     * @return Devuelve <code>true</code> si el directorio se borr&oacute; completamente, <code>false</code> si no se pudo borrar el directorio o
     *         alguno de los ficheros que contiene. */
    static boolean deleteDir(final File dir) {
        if (dir == null || !dir.exists()) {
            return true;
        }
        boolean success = true;

        // Si es un directorio, borramos su contenido
        if (dir.isDirectory()) {
            for (final File child : dir.listFiles()) {
                if (!deleteDir(child)) {
                    success = false;
                }
            }
        }

        // Borramos el propio fichero o directorio
        if (!dir.delete()) {
            LOGGER.severe("No se ha podido eliminar el archivo: " + dir); //$NON-NLS-1$
            success = false;
        }
        return success;
    }

    /** Realiza la instalaci&oacute;n de un fichero en el sistema local. Si se indica una
     * CA de firma se comprueba que el fichero ZIP/JAR este firmado con un certificado
     * expedido por esa CA.
     * @param remoteFile Ruta del fichero a instalar.
     * @param installationFile Ruta en donde se instalar&aacute;a el fichero.
     * @param signingCa CA que se debe verificar. Nulo cuando no proceda.
     * @throws URISyntaxException Si la URI proporcionada no tiene una sintaxis v&aacute;lida
     * @throws IOException Si ocurre un error de entrada/salida
     * @throws SecurityException Cuando ocurre un error al validar la firma del fichero */
    static void installFile(final URL remoteFile, final File installationFile, final SigningCert signingCa) throws IOException, URISyntaxException {

        if (signingCa == null) {
            copyFileFromURL(remoteFile, installationFile);
        }
        else {
            // Descargamos el fichero a un temporal, comprobamos la firma y
            // lo enviamos a la ruta de destino
            final File tempFile = createTempFile();
            copyFileFromURL(remoteFile, tempFile);
            checkSign(tempFile, signingCa);
            AOBootUtil.copyFile(tempFile, installationFile);
            try {
                tempFile.delete();
            }
            catch (final Exception e) {
                // Ignoramos los errores en el borrado, es responsabilidad del usuario limpiar periodicamente los temporales
            }
        }
    }

    /** Realiza la instalaci&oacute;n del contenido de un fichero Zip en el sistema local.
     * Si se indica una CA de firma se comprueba que el Zip est&eacute; firmado (firma JAR)
     * con un certificado expedido por esa CA.
     * @param remoteFile Nombre del fichero a instalar.
     * @param installationDir Directorio local en donde descomprimir el Zip.
     * @param signingCa CA que se debe verificar.
     * @throws URISyntaxException Si la URI proporcionada no tiene una sintaxis v&aacute;lida
     * @throws IOException Si ocurre un error de entrada/salida
     * @throws SecurityException Cuando ocurre un error al validar la firma del fichero */
    static void installZip(final URL remoteFile, final File installationDir, final SigningCert signingCa) throws IOException, URISyntaxException {
        final File tempFile = createTempFile();
        copyFileFromURL(remoteFile, tempFile);
        if (signingCa != null) {
            checkSign(tempFile, signingCa);
        }
        AOInstallUtils.unzip(new ZipFile(tempFile), installationDir);
        tempFile.deleteOnExit();
    }

    /** Comprueba que la firma de un JAR haya sido realizada con un certificado de la entidad
     * indicada.
     * @param jarFile Fichero JAR/ZIP.
     * @param sCert Certificado que debe haberse usado para firmar el JAR.
     * @throws SecurityException Cuando se produce cualquier error durante el proceso. */
    private static void checkSign(final File jarFile, final SigningCert sCert) {
        if (AfirmaBootLoader.DEBUG) {
            LOGGER.severe("IMPORTANTE: Modo de depuracion, comprobaciones de firma desactivadas"); //$NON-NLS-1$
            return;
        }
        new AOJarVerifier().verifyJar(jarFile.getAbsolutePath(), sCert.getSigningCertificate());
    }

    private static void createDirectory(final File dir) {
        if (dir == null) {
            LOGGER.warning("Se ha pedido crear un directorio nulo, se ignorara la peticion"); //$NON-NLS-1$
            return;
        }
        if (dir.exists()) {
            if (!dir.canWrite()) {
                LOGGER.severe("El fichero/directorio '" + dir.getAbsolutePath() //$NON-NLS-1$
                                                         + "' ya existe, pero no se tienen derechos de escritura sobre el"); //$NON-NLS-1$
                return;
            }
            if (dir.isFile()) {
                if (dir.delete()) {
                    if (dir.mkdir()) {
                        LOGGER.warning("'" + dir.getAbsolutePath() //$NON-NLS-1$
                                 + "' ya existia como fichero, se ha borrado y se ha creado un directorio con el mismo nombre"); //$NON-NLS-1$
                        return;
                    }

                    LOGGER.severe("'" + dir.getAbsolutePath() //$NON-NLS-1$
                            + "' ya existia como fichero y se ha borrado, pero no se ha podido crear un directorio con el mismo nombre"); //$NON-NLS-1$
                    return;
                }
                LOGGER.severe("'" + dir.getAbsolutePath() + "' ya existe como fichero y no se ha podido borrar"); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
        }
        // Si no existe
        else {
            if (!dir.getParentFile().canWrite()) {
                LOGGER.severe("El directorio '" + dir.getParent() + "' no tiene permisos de escritura, se intentara de todas formas"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (!dir.mkdir()) {
                LOGGER.severe("No se ha podido crear el directorio '" + dir.getAbsolutePath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

}
