"""Prueba las reglas instaladas en Firebase Emulator Suite con dos cuentas ficticias."""
import json
import secrets
import unittest
import urllib.error
import urllib.request
from uuid import uuid4

AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo"
DOCUMENTOS = "http://127.0.0.1:8184/v1/projects/demo-teclab-api4/databases/(default)/documents"


def solicitar(metodo, url, cuerpo=None, token=None):
    datos = None if cuerpo is None else json.dumps(cuerpo).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=datos, method=metodo, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as respuesta:
            return respuesta.status, json.load(respuesta)
    except urllib.error.HTTPError as error:
        with error:
            return error.code, json.load(error)


def cuenta():
    status, sesion = solicitar("POST", AUTH, {"email": f"{uuid4().hex}@example.test",
        "password": secrets.token_urlsafe(24), "returnSecureToken": True})
    if status != 200:
        raise ValueError("El emulador de Authentication no creó la cuenta")
    return sesion["localId"], sesion["idToken"]


class ReglasPorCuenta(unittest.TestCase):
    def test_solo_el_dueno_puede_leer_y_escribir(self):
        uid, a = cuenta()
        _, b = cuenta()
        doc = f"{DOCUMENTOS}/users/{uid}/reminders/15"
        original = {"fields": {"titulo": {"stringValue": "Revisar apuntes"}}}
        self.assertEqual(200, solicitar("PATCH", doc, original, a)[0])
        alterado = {"fields": {"titulo": {"stringValue": "Cambio de otra cuenta"}}}
        for token in (b, None):
            for metodo, datos in (("GET", None), ("PATCH", alterado), ("DELETE", None)):
                with self.subTest(metodo=metodo, autenticado=token is not None):
                    status, respuesta = solicitar(metodo, doc, datos, token)
                    self.assertEqual(403, status)
                    self.assertEqual("PERMISSION_DENIED", respuesta["error"]["status"])
        status, final = solicitar("GET", doc, token=a)
        self.assertEqual(200, status)
        self.assertEqual(original["fields"], final["fields"])
        self.assertEqual(200, solicitar("DELETE", doc, token=a)[0])


if __name__ == "__main__":
    unittest.main()
