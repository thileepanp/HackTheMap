# -*- coding: utf-8 -*-
"""
Created on Sat Nov 24 23:18:02 2018

@author: TPAULRAJ

This code uses a training data set with 1798 english scentences mapped to 10 
different classes, to train a text classifier. A neural network with 2 hidden 
layers is trained to classify this data according to it's classes. 

"""

from importlib import reload
import numpy as np
import sys
from imp import reload
import tensorflow as tf
import warnings
import re
from nltk.stem import WordNetLemmatizer
from nltk.corpus import stopwords
import pandas as pd
import keras
from keras.preprocessing.text import Tokenizer
from keras.preprocessing.sequence import pad_sequences
from keras.layers import Dense
from keras.models import Model, Sequential
from keras.layers import Convolution1D
from keras import initializers, regularizers, constraints, optimizers, layers
from keras.optimizers import SGD
from sklearn.model_selection import train_test_split 
from sklearn.metrics import f1_score, confusion_matrix 
from sklearn.metrics import classification_report
from sklearn.metrics import confusion_matrix
from sklearn.metrics import accuracy_score


#Reading training data that contains the text and the classes
text_data_from_driver = pd.read_csv('final_driver_data.csv', delimiter=',' 
                                    ,encoding='latin-1')
driver_text_data = text_data_from_driver.iloc[:,2:4] #removing the index column from the dataframe

#..............Data Preprocessing..............................................

stop_words = set(stopwords.words('english')) #contains 179 stopwords in english
lemmatizer = WordNetLemmatizer() #Used for lemmetizing english words 
    #Find more info about lemmetizing here 
    #https://textminingonline.com/dive-into-nltk-part-iv-stemming-and-lemmatization 

    #The following function does the following 1. removes unnecessary punctuation
    # 2. converts text to lower case 3. lemmetize words and puts the remaining 
    # words in a list called text and returs the list
    
def clean_text(text):
   text = re.sub(r'[^\w\s]','',text, re.UNICODE)
   text = text.lower()
   text = [lemmatizer.lemmatize(token) for token in text.split(' ')]
   text = [lemmatizer.lemmatize(token, 'v') for token in text]
   text = [word for word in text if not word in stop_words]
   text = ' '.join(text)
   return text

    #The clean_text function is applied to all the text in the data set
driver_text_data['text'] = driver_text_data.text.apply(lambda x: clean_text(x))

max_features = 6000000 #defining the size of the vocabilary to be tokenized
tokenizer = Tokenizer(num_words=max_features) #creating a tokenizer object
tokenizer.fit_on_texts(driver_text_data['text']) #fitting the tokenizer object on text
list_tokenized_train = tokenizer.texts_to_sequences(driver_text_data['text'])
    # Creating a list of integer tokens for each scentence 
    
#..............................................................................
        
#.........model building.......................................................
maxlen = 30 # All sequence in the input list will have a length of 30
X = np.array(pad_sequences(list_tokenized_train, maxlen=maxlen))  
    #Converting the tokenized training list to a numpy array of dimension (1798,30)
y = np.array(driver_text_data['labels']) # converting the classes into a numpy array of 
    #dimension (1798,1)

model = Sequential() #Creating a linearly layerable model
    #The first hidden layer will have 30 inpouts and 15 neurons
model.add(Dense(15, input_dim=30, activation='tanh'))
model.add(Dense(12, activation='relu')) # second hidden layer with 12 neurons
model.add(Dense(12, activation='relu')) #third hidden layer with 12 neurons
model.add(Dense(10, activation='softmax')) #output layer with 10 neurons for 10 classes

model.compile(optimizer='rmsprop', loss='sparse_categorical_crossentropy') 

    #............Data preperation.............................................
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.4)
    #split input data into train and test data 60% train and 40% test
model.fit(X_train, y_train, nb_epoch=50,validation_split=0.3)
    #fitting the model on the data using 30% of it for cross validation
y_pred = np.array(model.predict_classes(X_test)) # using the trained model
    # to predict outputs for the test data
y_pred = y_pred.reshape(-1) # reshaping the data into an array
print(y_pred) # printing the predicted values

#..............................................................................

#...........Model Evaluation metrics...........................................

print('F1-score: {0}'.format(f1_score(y_pred, y_test, average = 'micro')))  
    #Printing F1 score
print('Confusion matrix:')
confusion_matrix(y_pred, y_test) # printing confusion matrix to all the classes
print(classification_report(y_test,y_pred)) #printing precision, recall, F1 score
    # and report for all 10 classes
print ('Accuracy of this model is ',int(100*accuracy_score(y_test,y_pred)),'percent')
    # Printing accuracy of the model
