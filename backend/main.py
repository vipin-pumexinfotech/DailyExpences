from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, EmailStr
from typing import List, Optional
import time
import jwt

app = FastAPI(
    title="Zenith Finance REST API",
    description="Python FastAPI backend service supporting Authentication, Income & Expense Tracking, Recurring Expenses, and JSON Backup sync.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SECRET_KEY = "zenith_secret_key_change_in_production"
ALGORITHM = "HS256"

# Models
class UserRegister(BaseModel):
    name: str
    email: EmailStr
    password: str

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class GoogleAuthRequest(BaseModel):
    id_token: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict

class TransactionItem(BaseModel):
    id: Optional[int] = None
    title: str
    amount: float
    type: str  # "INCOME" or "EXPENSE"
    categoryId: int
    categoryName: str
    categoryIconName: str
    categoryColorHex: str
    timestamp: int
    note: str = ""
    isRecurring: bool = False
    recurringEndDate: Optional[int] = None
    recurringFrequency: str = "Monthly"

class BackupData(BaseModel):
    appName: str = "Zenith Finance"
    exportedAt: int
    transactions: List[TransactionItem]

# In-memory database mock for quick deployment & demonstration
mock_users = {}
mock_transactions = []

@app.get("/")
def read_root():
    return {"status": "online", "service": "Zenith Finance FastAPI Backend", "version": "1.0.0"}

@app.post("/api/auth/register", response_model=TokenResponse)
def register(user: UserRegister):
    if user.email in mock_users:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    user_data = {"name": user.name, "email": user.email}
    mock_users[user.email] = {**user_data, "password": user.password}
    
    token = jwt.encode({"sub": user.email, "exp": int(time.time()) + 86400 * 30}, SECRET_KEY, algorithm=ALGORITHM)
    return TokenResponse(access_token=token, user=user_data)

@app.post("/api/auth/login", response_model=TokenResponse)
def login(user: UserLogin):
    existing = mock_users.get(user.email)
    if not existing or existing["password"] != user.password:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    user_data = {"name": existing["name"], "email": existing["email"]}
    token = jwt.encode({"sub": user.email, "exp": int(time.time()) + 86400 * 30}, SECRET_KEY, algorithm=ALGORITHM)
    return TokenResponse(access_token=token, user=user_data)

@app.post("/api/auth/google", response_model=TokenResponse)
def google_auth(req: GoogleAuthRequest):
    # Simulated Google OAuth verification
    user_data = {"name": "Google User", "email": "user@gmail.com"}
    token = jwt.encode({"sub": user_data["email"], "exp": int(time.time()) + 86400 * 30}, SECRET_KEY, algorithm=ALGORITHM)
    return TokenResponse(access_token=token, user=user_data)

@app.get("/api/transactions", response_model=List[TransactionItem])
def get_transactions():
    return mock_transactions

@app.post("/api/transactions", response_model=TransactionItem)
def create_transaction(tx: TransactionItem):
    tx_dict = tx.dict()
    if not tx_dict.get("id"):
        tx_dict["id"] = int(time.time() * 1000)
    mock_transactions.append(tx_dict)
    return tx_dict

@app.delete("/api/transactions/{tx_id}")
def delete_transaction(tx_id: int):
    global mock_transactions
    mock_transactions = [t for t in mock_transactions if t.get("id") != tx_id]
    return {"success": True, "message": f"Transaction {tx_id} deleted"}

@app.get("/api/recurring", response_model=List[TransactionItem])
def get_recurring_transactions():
    return [t for t in mock_transactions if t.get("isRecurring")]

@app.post("/api/backup/export", response_model=BackupData)
def export_backup():
    return BackupData(
        appName="Zenith Finance",
        exportedAt=int(time.time() * 1000),
        transactions=[TransactionItem(**t) for t in mock_transactions]
    )

@app.post("/api/backup/import")
def import_backup(data: BackupData):
    global mock_transactions
    new_items = [t.dict() for t in data.transactions]
    mock_transactions.extend(new_items)
    return {"success": True, "importedCount": len(new_items)}
