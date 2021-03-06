package es.gob.afirma.android.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.Arrays;

import es.gob.afirma.core.misc.Base64;

/** Gestor para el cifrado sim&eacute;trico de datos (para el servidor intermedio). */
public final class CypherDataManager {

	/** Car&aacute;cter utilizado para separar el padding agregado a los datos para cifrarlos y los propios datos
	 * cifrados en base64. */
	private static final char PADDING_CHAR_SEPARATOR = '.';


	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	/** Descifra datos.
	 * @param cipheredDataB64 Datos cifrados (en Base64)
	 * @param cipherKey Clave de descifrado
	 * @return Datos descifrados
	 * @throws InvalidKeyException
	 * @throws IllegalArgumentException
	 * @throws GeneralSecurityException
	 * @throws IOException */
	public static byte[] decipherData(final byte[] cipheredDataB64,
			                          final byte[] cipherKey) throws InvalidKeyException,
			                                                         IllegalArgumentException,
			                                                         GeneralSecurityException,
			                                                         IOException {
		final String recoveredData = new String(cipheredDataB64, DEFAULT_URL_ENCODING).replace("_", "/").replace("-", "+"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		byte[] decipheredData;
		if (cipherKey != null) {
			decipheredData = decipherData(recoveredData, cipherKey);
		}
		else {
			decipheredData = Base64.decode(recoveredData, true);
		}
		return decipheredData;
	}

	/** Descifra una cadena de datos. Esta cadena viene precedida por el n&uacute;mero de caracteres de padding que
	 * se agregaron y separado por un punto (.) de la cadena base 64 con los datos cifrados.
	 * @param data Cadena de datos con la forma: PADDING.CIPHERDATAB64.
	 * @param cipherKey Clave de cifrado.
	 * @return Datos descifrados.
	 * @throws InvalidKeyException Cuando la clave no es v&aacute;lida.
	 * @throws GeneralSecurityException Cuando falla el proceso de cifrado.
	 * @throws IllegalArgumentException Si los datos no se corresponden con un Base64 v&aacute;lido.
	 * @throws IOException Cuando ocurre un error en la decodificaci&oacute;n de los datos. */
	private static byte[] decipherData(final String data, final byte[] cipherKey) throws InvalidKeyException, GeneralSecurityException, IllegalArgumentException, IOException {

		int padding = 0;
		final int dotPos = data.indexOf(PADDING_CHAR_SEPARATOR);
		if (dotPos != -1) {
			padding = Integer.parseInt(data.substring(0, dotPos));
		}

		final byte[] decipheredData = DesCypher.decypher(
				Base64.decode(data.substring(dotPos + 1).replace('+', '-').replace('/', '_'), true),
				cipherKey);

		return padding == 0 ? decipheredData : Arrays.copyOf(decipheredData, decipheredData.length - padding);
	}

	/** Genera una cadena con datos cifrados y codificados en base 64 antecedidos por el n&uacute;mero de
	 * caracteres que se han tenido que agregar como padding y separados por un car&aacute;cter separador.
	 * @param data Datos a cifrar.
	 * @param cipherKey Clave de cifrado.
	 * @return Cadena con el numero de caracteres agregados manualmente para cumplir la longitud requerida,
	 * el caracter separador y los datos cifrados y en base 64.
	 * @throws InvalidKeyException Cuando la clave no es v&aacute;lida.
	 * @throws GeneralSecurityException Cuando falla el proceso de cifrado.
	 * @throws IOException */
	public static String cipherData(final byte[] data, final byte[] cipherKey) throws InvalidKeyException, GeneralSecurityException, IOException {
		return
			Integer.toString(
				(DesCypher.getBlockSize() - data.length % DesCypher.getBlockSize()) % DesCypher.getBlockSize()
			) +
			PADDING_CHAR_SEPARATOR +
			Base64.encode(
				DesCypher.cypher(data, cipherKey),
				true
			);
	}
}
