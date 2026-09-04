from pydantic import BaseModel
from datetime import datetime

class PredictRequest(BaseModel):
    task_id: str
    text: str
    deadline: datetime

class PredictResponse(BaseModel):
    likelihood: float
