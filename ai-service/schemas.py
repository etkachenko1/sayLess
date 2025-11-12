from pydantic import BaseModel
from datetime import datetime

class PredictRequest(BaseModel):
    user_id: str
    text: str
    deadline: datetime
    assigned_by: str

class PredictResponse(BaseModel):
    likelihood: float
