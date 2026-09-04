#!/usr/bin/env python3
"""Seeds a running clinic-flow instance with a small, realistic clinic.

Goes through the API itself — POST /v1/patients, /v1/doctors, /v1/procedures,
/v1/appointments — not a SQL script against the database directly. Every row
this produces has therefore already passed brdoc's validation and Postgres's
own double-booking constraint, the same as a real registration would; a raw
INSERT script would drift from those rules the moment either one changes.

No real person's data anywhere in this file:

  - CPFs are generated here with the same public mod-11 check-digit algorithm
    brdoc itself implements (see validate/checkdigit.go in that repo) —
    algorithmically valid, structurally indistinguishable from a real one,
    and not looked up from or checked against any registry of real people.
    This is the standard way to produce Brazilian test data; it is what a
    "valid mock CPF" means.
  - Postcodes are real — Avenida Paulista, Copacabana, the Esplanada dos
    Ministérios — because ViaCEP's postcode-to-address directory is public
    infrastructure data, not personal data, the same distinction the rest of
    this project already draws for AddressLookupService.
  - Names are common, generic Portuguese given names and surnames, picked for
    variety, not to represent anyone real.

Usage:
    python3 scripts/seed.py [base_url]

    base_url defaults to http://localhost:8080.
"""

import json
import random
import sys
import urllib.error
import urllib.request

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"

FIRST_NAMES = [
    "Ana", "Bruno", "Carla", "Diego", "Elisa", "Fabio", "Gabriela", "Hugo",
    "Isabela", "Joao", "Larissa", "Marcos", "Natalia", "Otavio", "Paula",
    "Rafael", "Sofia", "Thiago", "Vanessa", "Wagner",
]
LAST_NAMES = [
    "Almeida", "Barbosa", "Costa", "Dias", "Ferreira", "Gomes", "Lima",
    "Martins", "Nascimento", "Oliveira", "Pereira", "Ribeiro", "Santos",
    "Souza", "Teixeira",
]

# Real, public postcodes — landmark addresses across a few Brazilian cities,
# so seeded patients and doctors are not all in the same neighbourhood.
POSTCODES = [
    "01310-200",  # Avenida Paulista, Sao Paulo
    "22070-011",  # Copacabana, Rio de Janeiro
    "70040-010",  # Esplanada dos Ministerios, Brasilia
    "40026-010",  # Comercio, Salvador
    "80010-000",  # Centro, Curitiba
    "90010-150",  # Centro Historico, Porto Alegre
    "50030-230",  # Boa Vista, Recife
    "30130-010",  # Centro, Belo Horizonte
]

SPECIALTIES = [
    "Cardiology", "Dermatology", "Pediatrics", "Orthopedics", "Gynecology",
    "Neurology", "Psychiatry", "Endocrinology",
]

PROCEDURES = [
    {"name": "Consultation", "durationMinutes": 30, "priceCents": 15000},
    {"name": "Follow-up visit", "durationMinutes": 15, "priceCents": 8000},
    {"name": "Complete blood panel", "durationMinutes": 20, "priceCents": 12000},
    {"name": "Electrocardiogram", "durationMinutes": 25, "priceCents": 18000},
    {"name": "Vaccination", "durationMinutes": 10, "priceCents": 6000},
]


def generate_cpf() -> str:
    """A structurally valid CPF — same mod-11 algorithm brdoc validates
    against — built from nine random digits, never looked up against a
    registry of real people."""
    digits = [random.randint(0, 9) for _ in range(9)]
    digits.append(_check_digit(digits, start_weight=10))
    digits.append(_check_digit(digits, start_weight=11))
    return "".join(str(d) for d in digits)


def _check_digit(digits: list[int], start_weight: int) -> int:
    total = sum(d * w for d, w in zip(digits, range(start_weight, 1, -1)))
    remainder = total % 11
    return 0 if remainder < 2 else 11 - remainder


def post(path: str, body: dict) -> dict:
    request = urllib.request.Request(
        f"{BASE_URL}{path}",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=25) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"POST {path} -> {error.code}: {detail}") from error


def seed_patients(count: int) -> list[str]:
    ids = []
    for _ in range(count):
        first, last = random.choice(FIRST_NAMES), random.choice(LAST_NAMES)
        patient = post(
            "/v1/patients",
            {
                "fullName": f"{first} {last}",
                "cpf": generate_cpf(),
                "email": f"{first.lower()}.{last.lower()}@example.com",
                "postcode": random.choice(POSTCODES),
            },
        )
        ids.append(patient["id"])
        print(f"  patient  {patient['fullName']:<24} {patient['id']}")
    return ids


def seed_doctors(count: int) -> list[str]:
    ids = []
    for i in range(count):
        first, last = random.choice(FIRST_NAMES), random.choice(LAST_NAMES)
        specialty = SPECIALTIES[i % len(SPECIALTIES)]
        doctor = post(
            "/v1/doctors",
            {
                "fullName": f"Dr. {first} {last}",
                "cpf": generate_cpf(),
                "email": f"dr.{first.lower()}.{last.lower()}@example.com",
                "specialty": specialty,
                # Not a real CRM — a plausible-shaped one: five digits, a
                # state, matching what CreateDoctorRequest expects and
                # nothing more, since it has no check-digit algorithm to
                # satisfy in the first place (see Doctor.licenseNumber).
                "licenseNumber": f"{10000 + i}-SP",
            },
        )
        ids.append(doctor["id"])
        print(f"  doctor   {doctor['fullName']:<24} {specialty:<14} {doctor['id']}")
    return ids


def seed_procedures() -> list[str]:
    ids = []
    for procedure in PROCEDURES:
        created = post("/v1/procedures", procedure)
        ids.append(created["id"])
        print(f"  procedure {created['name']:<24} {created['id']}")
    return ids


def seed_appointments(patient_ids: list[str], doctor_ids: list[str], procedure_ids: list[str], count: int) -> None:
    scheduled = 0
    attempts = 0
    while scheduled < count and attempts < count * 4:
        attempts += 1
        day = random.randint(1, 20)
        hour = random.choice([9, 10, 11, 14, 15, 16])
        body = {
            "patientId": random.choice(patient_ids),
            "doctorId": random.choice(doctor_ids),
            "procedureId": random.choice(procedure_ids),
            "startsAt": f"2026-10-{day:02d}T{hour:02d}:00:00Z",
        }
        try:
            appointment = post("/v1/appointments", body)
        except RuntimeError as error:
            # A double-booked slot or a past date is an expected outcome of
            # picking randomly, not a script bug — skip and try another.
            if "409" in str(error) or "422" in str(error):
                continue
            raise
        scheduled += 1
        print(f"  appointment {appointment['startsAt']}  {appointment['id']}")


def main() -> None:
    print(f"Seeding {BASE_URL} ...")
    print("procedures:")
    procedure_ids = seed_procedures()
    print("doctors:")
    doctor_ids = seed_doctors(8)
    print("patients:")
    patient_ids = seed_patients(15)
    print("appointments:")
    seed_appointments(patient_ids, doctor_ids, procedure_ids, count=12)
    print("Done.")


if __name__ == "__main__":
    main()
