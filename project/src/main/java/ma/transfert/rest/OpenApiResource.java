package ma.transfert.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/openapi")
public class OpenApiResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getOpenApi() {
        String yaml = "openapi: 3.0.3\n" +
                "info:\n" +
                "  title: MoneyTransfer API\n" +
                "  description: API de transfert d'argent — Projet DEV-6\n" +
                "  version: 1.0.0\n" +
                "servers:\n" +
                "  - url: http://localhost:8080/money-transfer/api\n" +
                "tags:\n" +
                "  - name: Health\n" +
                "    description: Etat du serveur\n" +
                "  - name: Auth\n" +
                "    description: Connexion et inscription\n" +
                "  - name: Transferts\n" +
                "    description: Gestion des transferts d'argent\n" +
                "  - name: Documents\n" +
                "    description: Gestion des documents KYC\n" +
                "paths:\n" +
                "  /health:\n" +
                "    get:\n" +
                "      tags: [Health]\n" +
                "      summary: Statut de l'application\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Serveur operationnel\n" +
                "  /auth/login:\n" +
                "    post:\n" +
                "      tags: [Auth]\n" +
                "      summary: Connexion utilisateur\n" +
                "      requestBody:\n" +
                "        required: true\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                email:\n" +
                "                  type: string\n" +
                "                  example: agent@transferpro.ma\n" +
                "                password:\n" +
                "                  type: string\n" +
                "                  example: Admin1234!\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Token JWT retourne\n" +
                "        '401':\n" +
                "          description: Identifiants incorrects\n" +
                "  /auth/register:\n" +
                "    post:\n" +
                "      tags: [Auth]\n" +
                "      summary: Inscription\n" +
                "      responses:\n" +
                "        '201':\n" +
                "          description: Compte cree\n" +
                "        '400':\n" +
                "          description: Email deja utilise\n" +
                "  /transfers:\n" +
                "    post:\n" +
                "      tags: [Transferts]\n" +
                "      summary: Creer un transfert\n" +
                "      requestBody:\n" +
                "        required: true\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                amount:\n" +
                "                  type: number\n" +
                "                  example: 500\n" +
                "                senderName:\n" +
                "                  type: string\n" +
                "                  example: Ali Benali\n" +
                "                receiverName:\n" +
                "                  type: string\n" +
                "                  example: Sara Alami\n" +
                "                senderPhone:\n" +
                "                  type: string\n" +
                "                  example: '0612345678'\n" +
                "                receiverPhone:\n" +
                "                  type: string\n" +
                "                  example: '0698765432'\n" +
                "      responses:\n" +
                "        '201':\n" +
                "          description: Transfert cree, OTP envoye\n" +
                "        '400':\n" +
                "          description: Donnees invalides\n" +
                "  /transfers/{trackingCode}:\n" +
                "    get:\n" +
                "      tags: [Transferts]\n" +
                "      summary: Details d'un transfert\n" +
                "      parameters:\n" +
                "        - name: trackingCode\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "            example: TRF-20260608-001\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Transfert trouve\n" +
                "        '404':\n" +
                "          description: Transfert non trouve\n" +
                "  /transfers/{trackingCode}/confirm:\n" +
                "    put:\n" +
                "      tags: [Transferts]\n" +
                "      summary: Confirmer un transfert (PENDING -> CONFIRMED)\n" +
                "      parameters:\n" +
                "        - name: trackingCode\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Transfert confirme\n" +
                "  /transfers/{trackingCode}/pay:\n" +
                "    put:\n" +
                "      tags: [Transferts]\n" +
                "      summary: Payer un transfert avec OTP\n" +
                "      parameters:\n" +
                "        - name: trackingCode\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                otpCode:\n" +
                "                  type: string\n" +
                "                  example: '12345678'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Paiement effectue\n" +
                "        '400':\n" +
                "          description: OTP invalide\n" +
                "  /transfers/{trackingCode}/cancel:\n" +
                "    put:\n" +
                "      tags: [Transferts]\n" +
                "      summary: Annuler un transfert\n" +
                "      parameters:\n" +
                "        - name: trackingCode\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Transfert annule\n" +
                "  /documents/upload:\n" +
                "    post:\n" +
                "      tags: [Documents]\n" +
                "      summary: Upload d'un document KYC\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          multipart/form-data:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                file:\n" +
                "                  type: string\n" +
                "                  format: binary\n" +
                "                type:\n" +
                "                  type: string\n" +
                "                  example: CNI_RECTO\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Document uploade\n" +
                "        '400':\n" +
                "          description: Type de fichier refuse (gif, exe...)\n";

        return Response.ok(yaml).build();
    }
}