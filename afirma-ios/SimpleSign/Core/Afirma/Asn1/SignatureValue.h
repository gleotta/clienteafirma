/*
 * Generated by asn1c-0.9.22 (http://lionet.info/asn1c)
 * From ASN.1 module "SIGNEDDATA"
 * 	found in "SIGNEDDATA.asn1"
 * 	`asn1c -S/skeletons`
 */

#ifndef	_SignatureValue_H_
#define	_SignatureValue_H_


#include "asn_application.h"

/* Including external dependencies */
#include "OCTET_STRING.h"

#ifdef __cplusplus
extern "C" {
#endif

/* SignatureValue */
typedef OCTET_STRING_t	 SignatureValue_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_SignatureValue;
asn_struct_free_f SignatureValue_free;
asn_struct_print_f SignatureValue_print;
asn_constr_check_f SignatureValue_constraint;
ber_type_decoder_f SignatureValue_decode_ber;
der_type_encoder_f SignatureValue_encode_der;
xer_type_decoder_f SignatureValue_decode_xer;
xer_type_encoder_f SignatureValue_encode_xer;

#ifdef __cplusplus
}
#endif

#endif	/* _SignatureValue_H_ */
