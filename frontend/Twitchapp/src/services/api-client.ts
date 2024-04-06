import axios from "axios";
import { getUserId, getIsUserLoggedIn } from "./session";

const IS_AWS_BUILD = import.meta.env.VITE_IS_AWS_BUILD;

let domain = "http://localhost:8080";
// if (IS_AWS_BUILD) {
//   domain = import.meta.env.VITE_BACKEND_AWS_URL;
// }

const getHeaders = (): any => {
  return{
    userId: getUserId(),
    "Access-Control-Allow-Origin": "*",
    // "Access-Control-Allow-Methods" : "DELETE,GET,POST,PUT,OPTIONS",
    // "Access-Control-Allow-Headers" : "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
    // "Content-Type" : "application/json",
    // "Accept" : "application/json"
  }
}

export const getChannels = async () => {
  // console.log(domain);
  const response = await axios.get(domain + "/channels", {headers: getHeaders()});
  return response.data;
};

export const getTwitchAnalysisOfChannel = async (channel_name: any) => {
  const response = await axios.get(domain + "/twitch_analysis", {
    params: {
      channel_name: channel_name,
    },
  });
  return response.data;
};

export const logIn = async (name: any, password: any, isUserName: Boolean) => {
  let username, email;
  if (isUserName) {
    username = name;
  } else {
    email = name;
  }
  let responseAPI = await axios
    .post(domain + "/user/authenticate", {
      username,
      email,
      password,
    })
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      let data = error.response;
      data.isError = true;
      return data;
    });
  return responseAPI;
};

export const register = async (username: any, password: any, email: any) => {
  let responseAPI = await axios
    .post(domain + "/user/register", {
      username,
      email,
      password,
    },{
    headers: getHeaders()
    })
    .then((response) => {
      return response;
    })
    .catch((error) => {
      let data = error.response;
      data.isError = true;
      return data;
    });
  return responseAPI;
};

export const subscribeChannel = async(channel_name: any) => {
  let responseAPI = await axios
  .post(domain + "/subscribeChannel",{}, {
    headers: getHeaders(),
    params: {
      user_id : getUserId(),
      channel_name: channel_name,
    }
  })
  .then((response) => {
    return response;
  })
  .catch((error) => {
    let data = error.response;
    data.isError = true;
    return data;
  });
return responseAPI;
};

export const unSubscribeChannel = async(channel_name: any) => {
  let responseAPI = await axios
  .delete(domain + "/unSubscribeChannel", {
    headers: getHeaders(),
    params: {
      user_id : getUserId(),
      channel_name: channel_name,
    }
  })
  .then((response) => {
    return response;
  })
  .catch((error) => {
    let data = error.response;
    data.isError = true;
    return data;
  });
return responseAPI;
};

export const getSubscribedChannels = async () => {};

export const searchChannel = async(channel_name:any) => {
    const response = await axios.get(domain + "/search", 
    { headers: getHeaders(),
      params:{
      channel_name : channel_name
     }
    });
     return response;
}